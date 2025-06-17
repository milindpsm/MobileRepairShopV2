package com.example.mobilerepairshopv2.data

import androidx.room.Transaction
import com.example.mobilerepairshopv2.data.local.OrderDao
import com.example.mobilerepairshopv2.data.local.RepairDao
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.OrderDashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.flow.Flow

/**
 * The Repository now manages both Repair and Order DAOs.
 */
class RepairRepository(private val repairDao: RepairDao, private val orderDao: OrderDao) {

    // --- Repair Functions ---
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

    // --- NEW: Order Functions ---
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    fun getOrderStatsForPeriod(startDate: Long, endDate: Long): Flow<OrderDashboardStats?> {
        return orderDao.getOrderStats(startDate, endDate)
    }



    fun getOrderById(id: Long): Flow<Order?> {
        return orderDao.getOrderById(id)
    }

    fun searchOrders(query: String): Flow<List<Order>> {
        return orderDao.searchOrders(query)
    }

    suspend fun insertOrder(order: Order) {
        orderDao.insert(order)
    }

    suspend fun updateOrder(order: Order) {
        orderDao.update(order)
    }

    suspend fun deleteOrder(order: Order) {
        orderDao.delete(order)
    }
}
