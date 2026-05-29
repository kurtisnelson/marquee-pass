package com.thisisnotajoke.marqueepass.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.database.PropertyName


@Keep
enum class ShowStatus {
    @PropertyName("SEEN") SEEN,
    @PropertyName("WANT_TO_SEE") WANT_TO_SEE
}

@Keep
@Entity(tableName = "shows")
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
