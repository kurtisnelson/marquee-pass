package com.thisisnotajoke.marqueepass.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.thisisnotajoke.marqueepass.data.Show
import com.thisisnotajoke.marqueepass.data.ShowRepository
import com.thisisnotajoke.marqueepass.data.ShowStatus
import com.thisisnotajoke.marqueepass.sync.FirebaseAuthManager
import com.thisisnotajoke.marqueepass.util.ConnectivityObserver
import com.thisisnotajoke.marqueepass.util.ConnectivityStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import com.thisisnotajoke.marqueepass.di.AppScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey(ShowViewModel::class)
class ShowViewModel(
    private val showRepository: ShowRepository,
    private val authManager: FirebaseAuthManager,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUserState

    val connectionStatus: StateFlow<ConnectivityStatus> = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectivityStatus.Available
        )

    // Share a single Firestore listener across both filtered flows.
    private val allShows = showRepository.observeAllShows()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    val seenShows: Flow<List<Show>> = allShows.map { shows ->
        shows.filter { it.status == ShowStatus.SEEN }
    }

    val wantToSeeShows: Flow<List<Show>> = allShows.map { shows ->
        shows.filter { it.status == ShowStatus.WANT_TO_SEE }
    }

    init {
        // Ensure the user is always authenticated (creates anonymous session if needed).
        viewModelScope.launch { authManager.ensureAuthenticated() }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CRUD — all writes go directly to Firestore; real-time listener
    // propagates changes back to the UI automatically.
    // ──────────────────────────────────────────────────────────────────────────

    fun addShow(show: Show) {
        val sanitized = sanitize(show)
        viewModelScope.launch { showRepository.addShow(sanitized) }
    }

    fun updateShow(show: Show) {
        val sanitized = sanitize(show)
        viewModelScope.launch { showRepository.updateShow(sanitized) }
    }

    fun deleteShow(show: Show) {
        viewModelScope.launch { showRepository.deleteShow(show.id) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Authentication
    // ──────────────────────────────────────────────────────────────────────────

    fun handleGoogleSignIn(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                tryLinkOrSignIn(idToken, onSuccess)
            } catch (e: Exception) {
                onError(e.message ?: "Google authentication failed")
            }
        }
    }

    /**
     * Attempts to link the anonymous account to Google first (preserves the
     * existing Firestore data path). On credential collision, performs a full
     * sign-in and merges any guest shows that were not already present in the
     * Google account's Firestore collection.
     */
    private suspend fun tryLinkOrSignIn(idToken: String, onSuccess: () -> Unit) {
        try {
            authManager.linkWithGoogle(idToken)
            onSuccess()
        } catch (linkException: Exception) {
            val isCollision = linkException is FirebaseAuthUserCollisionException
                || linkException.cause is FirebaseAuthUserCollisionException
                || linkException.message?.contains("credential already in use", ignoreCase = true) == true

            if (!isCollision) throw linkException

            // ── Collision flow ────────────────────────────────────────────────
            // 1. Capture the guest UID and their shows BEFORE switching auth.
            val guestUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("No guest session to merge from")
            val guestShows = showRepository.getShowsOnce(guestUid)

            // 2. Sign in with the Google account (auth state changes here).
            authManager.signInWithGoogle(idToken)

            // 3. Write each guest show into the Google account's collection with
            //    a new Firestore-generated ID (clears the old anonymous doc ID).
            guestShows.forEach { guestShow ->
                val sanitized = sanitize(guestShow).copy(id = "")
                showRepository.addShow(sanitized)
            }
            // The Firestore listener in ShowRepository automatically switches
            // to the new uid's collection, so no manual UI refresh is needed.
            onSuccess()
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // signOut() in FirebaseAuthManager also creates a fresh anonymous
                // session, so the Firestore listener re-attaches immediately.
                authManager.signOut()
                onSuccess()
            } catch (e: Exception) {
                // Sign-out errors are non-fatal; report but don't crash.
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Ensures wishlist shows never carry a date or rating. */
    private fun sanitize(show: Show): Show =
        if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null, rating = null)
        else show
}
