package com.thisisnotajoke.marqueepass.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows ORDER BY date DESC")
    fun getAllShows(): Flow<List<Show>>

    @Query("SELECT * FROM shows WHERE status = :status ORDER BY date DESC")
    fun getShowsByStatus(status: ShowStatus): Flow<List<Show>>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: Int): Show?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: Show): Long

    @Update
    suspend fun updateShow(show: Show)

    @Delete
    suspend fun deleteShow(show: Show)

    @Query("DELETE FROM shows")
    suspend fun deleteAllShows()
}
