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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShowViewModel(application: Application) : AndroidViewModel(application) {
    private val showDao = AppDatabase.getDatabase(application).showDao()
    private val firebaseSync = FirebaseSyncManager()
    private val connectivityObserver = ConnectivityObserver(application)

    val currentUser: StateFlow<FirebaseUser?> = firebaseSync.currentUserState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val seenShows: Flow<List<Show>> = showDao.getShowsByStatus(ShowStatus.SEEN)
    val wantToSeeShows: Flow<List<Show>> = showDao.getShowsByStatus(ShowStatus.WANT_TO_SEE)
    val ticketedShows: Flow<List<Show>> = showDao.getShowsByStatus(ShowStatus.TICKETED)

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
                    showDao.insertShow(show)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun syncAllToFirebase() {
        viewModelScope.launch {
            showDao.getAllShows().collectLatest { allShows ->
                allShows.forEach { show ->
                    firebaseSync.syncShow(show)
                }
            }
        }
    }

    fun addShow(show: Show) {
        viewModelScope.launch {
            val id = showDao.insertShow(show).toInt()
            firebaseSync.syncShow(show.copy(id = id))
        }
    }

    fun updateShow(show: Show) {
        viewModelScope.launch {
            showDao.updateShow(show)
            firebaseSync.syncShow(show)
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
                        // 2. Already linked to another account -> merge and sign in
                        val currentLocalShows = showDao.getAllShows().first()
                        
                        firebaseSync.signInWithGoogle(idToken)
                        
                        // Clear Room cache of temporary guest
                        showDao.deleteAllShows()
                        
                        // Fetch existing permanent account's shows from Firebase
                        val remoteShows = firebaseSync.fetchAllShows()
                        remoteShows.forEach { show ->
                            showDao.insertShow(show)
                        }
                        
                        // Sync current guest shows into the signed-in account (merge)
                        currentLocalShows.forEach { show ->
                            val localId = showDao.insertShow(show).toInt()
                            firebaseSync.syncShow(show.copy(id = localId))
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
