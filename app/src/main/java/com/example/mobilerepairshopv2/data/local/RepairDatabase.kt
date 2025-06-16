package com.example.mobilerepairshopv2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.Repair

// --- MODIFIED: Added Order::class to entities and bumped version to 4 ---
@Database(entities = [Repair::class, Order::class], version = 4, exportSchema = false)
abstract class RepairDatabase : RoomDatabase() {

    // --- MODIFIED: Added an abstract function for the new OrderDao ---
    abstract fun repairDao(): RepairDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: RepairDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repairs_table ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }

        // This is an empty migration for the nullable fields change we did earlier.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Room can handle changing columns to nullable automatically if the data is just cleared.
                // For a more complex migration, you would alter the table here.
                // Since we used fallbackToDestructiveMigration before, this is for good practice.
            }
        }

        // --- NEW: Migration from version 3 to 4 to add the new orders_table ---
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `orders_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerName` TEXT,
                        `customerContact` TEXT NOT NULL,
                        `imagePath` TEXT,
                        `description` TEXT,
                        `totalCost` REAL NOT NULL,
                        `advanceTaken` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `dateAdded` INTEGER NOT NULL,
                        `dateCompleted` INTEGER
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): RepairDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RepairDatabase::class.java,
                    "repair_database"
                )
                    // --- MODIFIED: Added the new migration path ---
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
