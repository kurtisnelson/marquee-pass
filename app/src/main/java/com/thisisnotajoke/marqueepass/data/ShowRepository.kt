package com.thisisnotajoke.marqueepass.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.thisisnotajoke.marqueepass.di.AppScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository that owns all Firestore CRUD for shows.
 *
 * Data is stored at: users/{uid}/shows/{showId}
 *
 * [observeAllShows] returns a real-time [Flow] that automatically
 * re-attaches the Firestore listener whenever the auth user changes
 * (anonymous → Google sign-in, sign-out, etc.).
 */
@SingleIn(AppScope::class)
class ShowRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ShowRepository"
    }

    private fun showsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("shows")

    // ──────────────────────────────────────────────────────────────────────────
    // Real-time observation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Emits the full list of shows for the current user every time Firestore
     * sends an update. Automatically reconnects when the auth user changes.
     *
     * Emits [emptyList] when no user is signed in.
     */
    fun observeAllShows(): Flow<List<Show>> = callbackFlow {
        var firestoreListener: ListenerRegistration? = null

        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            // Tear down any previous listener before attaching the new one.
            firestoreListener?.remove()
            val uid = auth.currentUser?.uid
            if (uid == null) {
                trySend(emptyList())
                return@AuthStateListener
            }
            firestoreListener = showsCollection(uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to shows for uid=$uid", error)
                    return@addSnapshotListener
                }
                val shows = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject<Show>() }.getOrNull()
                } ?: emptyList()
                trySend(shows)
            }
        }

        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener(authStateListener)

        awaitClose {
            firestoreListener?.remove()
            auth.removeAuthStateListener(authStateListener)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // One-shot fetch (used during sign-in collision merge)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the shows for an arbitrary [uid] once without a live listener.
     * Used to read guest data before switching auth users.
     */
    suspend fun getShowsOnce(uid: String): List<Show> = try {
        val snapshot = showsCollection(uid).get().await()
        snapshot.documents.mapNotNull { doc ->
            runCatching { doc.toObject<Show>() }.getOrNull()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch shows once for uid=$uid", e)
        emptyList()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Writes
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new show for [targetUid] (defaults to current user).
     * Returns the generated document ID, or null on failure.
     */
    suspend fun addShow(show: Show, targetUid: String? = null): String? {
        val uid = targetUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return try {
            val ref = showsCollection(uid).add(show).await()
            ref.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add show", e)
            null
        }
    }

    /**
     * Overwrites an existing show document identified by [show.id].
     * No-op if [show.id] is blank.
     */
    suspend fun updateShow(show: Show) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (show.id.isBlank()) return
        try {
            showsCollection(uid).document(show.id).set(show).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update show ${show.id}", e)
        }
    }

    /**
     * Deletes the show with the given [showId].
     */
    suspend fun deleteShow(showId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            showsCollection(uid).document(showId).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete show $showId", e)
        }
    }
}
