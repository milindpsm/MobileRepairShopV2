package com.example.mobilerepairshopv2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mobilerepairshopv2.data.model.Repair

// IMPORTANT: Increased version number from 2 to 3
@Database(entities = [Repair::class], version = 3, exportSchema = false)
abstract class RepairDatabase : RoomDatabase() {

    abstract fun repairDao(): RepairDao

    companion object {
        @Volatile
        private var INSTANCE: RepairDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repairs_table ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }

        // This is an empty migration from 2 to 3, as Room can handle nullable changes automatically.
        // We will use fallbackToDestructiveMigration instead, which is simpler for development.
        // private val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         // No SQL needed for changing column to nullable
        //     }
        // }

        fun getDatabase(context: Context): RepairDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RepairDatabase::class.java,
                    "repair_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    // NEW: This tells Room to recreate the database if a migration is not found.
                    // This is a safe and easy way to handle schema changes during development.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
