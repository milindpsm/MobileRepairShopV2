package com.example.mobilerepairshopv2

import android.app.Application
import com.example.mobilerepairshopv2.data.RepairRepository
import com.example.mobilerepairshopv2.data.local.RepairDatabase

// This class now only holds the database and repository instances.
class RepairShopApplication : Application() {
    val database by lazy { RepairDatabase.getDatabase(this) }
    val repository by lazy { RepairRepository(database.repairDao()) }
}
    