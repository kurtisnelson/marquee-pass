package com.thisisnotajoke.marqueepass.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.thisisnotajoke.marqueepass.data.Show
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager {
    companion object {
        private var isPersistenceEnabled = false
        
        fun getDatabase(): FirebaseDatabase {
            val db = FirebaseDatabase.getInstance()
            if (!isPersistenceEnabled) {
                try {
                    db.setPersistenceEnabled(true)
                    isPersistenceEnabled = true
                } catch (e: Exception) {
                    Log.w("FirebaseSyncManager", "Could not set persistence enabled", e)
                }
            }
            return db
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = getDatabase()
    
    private val showsRef = database.getReference("users").apply {
        keepSynced(true)
    }

    private val _currentUserState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUserState: StateFlow<FirebaseUser?> = _currentUserState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserState.value = firebaseAuth.currentUser
        }
    }

    suspend fun ensureAuthenticated(): String? {
        val currentUser = auth.currentUser
        if (currentUser != null) return currentUser.uid

        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Anonymous auth failed", e)
            null
        }
    }

    suspend fun syncShow(show: Show) {
        val userId = ensureAuthenticated() ?: return
        try {
            showsRef.child(userId).child("shows").child(show.id.toString()).setValue(show).await()
            Log.d("FirebaseSyncManager", "Synced show ${show.id} to Firebase")
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Failed to sync show ${show.id}", e)
        }
    }

    suspend fun deleteShow(showId: Int) {
        val userId = ensureAuthenticated() ?: return
        try {
            showsRef.child(userId).child("shows").child(showId.toString()).removeValue().await()
            Log.d("FirebaseSyncManager", "Deleted show $showId from Firebase")
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Failed to delete show $showId", e)
        }
    }

    suspend fun fetchAllShows(): List<Show> {
        val userId = ensureAuthenticated() ?: return emptyList()
        return try {
            val snapshot = showsRef.child(userId).child("shows").get().await()
            snapshot.children.mapNotNull { it.getValue(Show::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseSyncManager", "Failed to fetch shows from Firebase", e)
            emptyList()
        }
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw Exception("Sign in failed - no user returned")
    }

    suspend fun linkWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = auth.currentUser ?: throw Exception("No active guest account found to link to.")
        val result = currentUser.linkWithCredential(credential).await()
        return result.user ?: throw Exception("Linking failed - no user returned")
    }

    suspend fun signOut() {
        auth.signOut()
        ensureAuthenticated()
    }
}
