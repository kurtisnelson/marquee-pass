package com.thisisnotajoke.marqueepass.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.thisisnotajoke.marqueepass.data.AppDatabase
import com.thisisnotajoke.marqueepass.data.Show
import com.thisisnotajoke.marqueepass.data.ShowStatus
import com.thisisnotajoke.marqueepass.sync.FirebaseSyncManager
import com.thisisnotajoke.marqueepass.util.ConnectivityObserver
import com.thisisnotajoke.marqueepass.util.ConnectivityStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import com.thisisnotajoke.marqueepass.di.AppScope
import com.thisisnotajoke.marqueepass.data.ShowDao

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey(ShowViewModel::class)
class ShowViewModel(
    private val showDao: ShowDao,
    private val firebaseSync: FirebaseSyncManager,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = firebaseSync.currentUserState

    val connectionStatus: StateFlow<ConnectivityStatus> = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectivityStatus.Available
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val seenShows: Flow<List<Show>> = showDao.getShowsByStatus(ShowStatus.SEEN)
    val wantToSeeShows: Flow<List<Show>> = showDao.getShowsByStatus(ShowStatus.WANT_TO_SEE)

    init {
        // Monitor connectivity and trigger a full sync when network becomes available
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityStatus.Available) {
                    syncAllToFirebase()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Fetch from Firebase and update Room
                val remoteShows = firebaseSync.fetchAllShows()
                remoteShows.forEach { show ->
                    val sanitized = if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null) else show
                    showDao.insertShow(sanitized)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun syncAllToFirebase() {
        try {
            val allShows = showDao.getAllShows().first()
            allShows.forEach { show ->
                firebaseSync.syncShow(show)
            }
        } catch (e: Exception) {
            // Log but don't crash - sync is best-effort
        }
    }

    fun addShow(show: Show) {
        viewModelScope.launch {
            val sanitizedShow = if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null) else show
            val showWithId = if (sanitizedShow.id == 0L) sanitizedShow.copy(id = System.currentTimeMillis()) else sanitizedShow
            showDao.insertShow(showWithId)
            firebaseSync.syncShow(showWithId)
        }
    }

    fun updateShow(show: Show) {
        viewModelScope.launch {
            val sanitizedShow = if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null) else show
            showDao.updateShow(sanitizedShow)
            firebaseSync.syncShow(sanitizedShow)
        }
    }

    fun deleteShow(show: Show) {
        viewModelScope.launch {
            showDao.deleteShow(show)
            firebaseSync.deleteShow(show.id)
        }
    }

    fun handleGoogleSignIn(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                try {
                    // Try to link first (preserves local data without migration if Google account is new to Firebase)
                    firebaseSync.linkWithGoogle(idToken)
                    onSuccess()
                } catch (e: Exception) {
                    val isCollision = e is FirebaseAuthUserCollisionException || 
                                     e.cause is FirebaseAuthUserCollisionException || 
                                     e.message?.contains("credential already in use", ignoreCase = true) == true
                    
                    if (isCollision) {
                        // 1. Save local shows BEFORE any destructive operations
                        val currentLocalShows = showDao.getAllShows().first()
                        
                        // 2. Sign in to existing Google account
                        firebaseSync.signInWithGoogle(idToken)
                        
                        // 3. Fetch remote shows BEFORE deleting local data
                        val remoteShows = firebaseSync.fetchAllShows()
                        
                        // 4. NOW safe to clear and rebuild local database
                        showDao.deleteAllShows()
                        
                        // 5. Insert remote shows first
                        remoteShows.forEach { show ->
                            val sanitized = if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null) else show
                            showDao.insertShow(sanitized)
                        }
                        
                        // 6. Merge local guest shows with unique IDs to avoid collisions
                        currentLocalShows.forEachIndexed { index, show ->
                            val sanitized = if (show.status == ShowStatus.WANT_TO_SEE) show.copy(date = null) else show
                            val mergedShow = sanitized.copy(id = System.currentTimeMillis() + index)
                            showDao.insertShow(mergedShow)
                            firebaseSync.syncShow(mergedShow)
                        }
                        onSuccess()
                    } else {
                        throw e
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Google authentication failed")
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                firebaseSync.signOut()
                showDao.deleteAllShows()
                onSuccess()
            } catch (e: Exception) {
                // Ignore sign out exceptions or handle gracefully
            }
        }
    }
}
