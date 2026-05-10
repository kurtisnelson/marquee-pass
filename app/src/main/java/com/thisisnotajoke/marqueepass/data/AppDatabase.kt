package com.thisisnotajoke.marqueepass.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromStatus(status: ShowStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(status: String): ShowStatus {
        return ShowStatus.valueOf(status)
    }
}

@Database(entities = [Show::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun showDao(): ShowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marquee_pass_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
