package com.thisisnotajoke.marqueepass.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.thisisnotajoke.marqueepass.di.AppScope

/**
 * Manages Firebase Authentication state: anonymous sign-in, Google
 * sign-in, Google account linking, and sign-out.
 *
 * All Realtime Database code has been removed — data is now owned
 * exclusively by [com.thisisnotajoke.marqueepass.data.ShowRepository]
 * via Cloud Firestore.
 */
@SingleIn(AppScope::class)
class FirebaseAuthManager @Inject constructor() {
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    private val auth = FirebaseAuth.getInstance()

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserState.value = firebaseAuth.currentUser
        }
    }

    /**
     * Returns the current user's UID, signing in anonymously if needed.
     */
    suspend fun ensureAuthenticated(): String? {
        auth.currentUser?.let { return it.uid }
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            null
        }
    }

    /**
     * Signs in with a Google ID token, replacing the current auth user.
     */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw Exception("Sign in failed — no user returned")
    }

    /**
     * Links the current anonymous user to a Google account.
     * Throws [com.google.firebase.auth.FirebaseAuthUserCollisionException]
     * if the Google account is already in use by another user.
     */
    suspend fun linkWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = auth.currentUser
            ?: throw Exception("No active guest account to link")
        val result = currentUser.linkWithCredential(credential).await()
        return result.user ?: throw Exception("Linking failed — no user returned")
    }

    /**
     * Signs out the current user, then immediately creates a new
     * anonymous session so the app is never left unauthenticated.
     */
    suspend fun signOut() {
        auth.signOut()
        ensureAuthenticated()
    }
}
