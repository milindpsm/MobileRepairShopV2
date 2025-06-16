package com.example.mobilerepairshopv2.data

import androidx.room.Transaction
import com.example.mobilerepairshopv2.data.local.RepairDao
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.flow.Flow

/**
 * The Repository class abstracts access to multiple data sources.
 * In this app, we only have one data source: the Room database.
 * It provides a clean API for data access to the rest of the application.
 */
class RepairRepository(private val repairDao: RepairDao) {

    val allRepairs: Flow<List<Repair>> = repairDao.getAllRepairsOrderedByDate()
    val pendingCount: Flow<Int> = repairDao.getPendingCount()

    fun getStatsForPeriod(startDate: Long, endDate: Long): Flow<DashboardStats?> {
        return repairDao.getStats(startDate, endDate)
    }

    fun searchRepairs(searchQuery: String): Flow<List<Repair>> {
        return repairDao.searchDatabase(searchQuery)
    }

    fun getRepairById(id: Long): Flow<Repair?> {
        return repairDao.getRepairById(id)
    }

    suspend fun insert(repair: Repair) {
        repairDao.insert(repair)
    }

    suspend fun update(repair: Repair) {
        repairDao.update(repair)
    }

    suspend fun delete(repair: Repair) {
        repairDao.delete(repair)
    }

    suspend fun getAllRepairsForBackup(): List<Repair> {
        return repairDao.getAllRepairsForBackup()
    }

    @Transaction
    suspend fun restoreFromBackup(repairs: List<Repair>) {
        repairDao.clearAll()
        repairs.forEach { repairDao.insert(it) }
    }
}
