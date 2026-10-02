package com.easycode.ide.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.easycode.ide.data.model.FiddleEntity

@Database(entities = [FiddleEntity::class], version = 1, exportSchema = false)
abstract class EasyCodeDatabase : RoomDatabase() {

    abstract fun fiddleDao(): FiddleDao

    companion object {
        @Volatile
        private var INSTANCE: EasyCodeDatabase? = null

        fun getInstance(context: Context): EasyCodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EasyCodeDatabase::class.java,
                    "easycode_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
