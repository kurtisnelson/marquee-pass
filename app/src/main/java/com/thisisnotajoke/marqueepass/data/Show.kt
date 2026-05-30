package com.thisisnotajoke.marqueepass.data

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
enum class ShowStatus {
    SEEN, WANT_TO_SEE
}

/**
 * Firestore document model for a show.
 *
 * The [id] field is annotated with [@DocumentId] so Firestore automatically
 * populates it from the document ID on reads but does NOT write it as a
 * stored field. All other fields are stored as-is.
 *
 * Collection path: users/{uid}/shows/{id}
 */
@Keep
data class Show(
    @DocumentId val id: String = "",
    val title: String = "",
    val theater: String? = null,
    /** Epoch millis. Only set when status == SEEN. */
    val date: Long? = null,
    val status: ShowStatus = ShowStatus.WANT_TO_SEE,
    /** 1–5 stars. Only set when status == SEEN. */
    val rating: Int? = null,
    val notes: String? = null
)
