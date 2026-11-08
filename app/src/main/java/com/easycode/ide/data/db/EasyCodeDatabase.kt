package com.easycode.ide.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.easycode.ide.data.model.DraftEntity
import com.easycode.ide.data.model.FiddleEntity

@Database(entities = [FiddleEntity::class, DraftEntity::class], version = 2, exportSchema = false)
abstract class EasyCodeDatabase : RoomDatabase() {

    abstract fun fiddleDao(): FiddleDao
    abstract fun draftDao(): DraftDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `drafts` (`id` INTEGER NOT NULL, `stateJson` TEXT NOT NULL, `savedSnapshotJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        @Volatile
        private var INSTANCE: EasyCodeDatabase? = null

        fun getInstance(context: Context): EasyCodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EasyCodeDatabase::class.java,
                    "easycode_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
