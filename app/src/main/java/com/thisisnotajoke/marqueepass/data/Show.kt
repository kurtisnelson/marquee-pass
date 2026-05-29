package com.thisisnotajoke.marqueepass.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class ShowStatus {
    SEEN,
    WANT_TO_SEE,
    TICKETED
}

@Entity(tableName = "shows")
@Serializable
data class Show(
    @PrimaryKey
    val id: Long = 0L,
    val title: String = "",
    val theater: String? = null,
    val date: Long? = null,
    val status: ShowStatus = ShowStatus.WANT_TO_SEE,
    val rating: Int? = null,
    val notes: String? = null
)
