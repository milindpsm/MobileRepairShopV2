package com.example.mobilerepairshopv2.data

import androidx.room.Transaction
import com.example.mobilerepairshopv2.data.local.OrderDao
import com.example.mobilerepairshopv2.data.local.RepairDao
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.OrderDashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.flow.Flow

class RepairRepository(private val repairDao: RepairDao, private val orderDao: OrderDao) {

    // --- Repair Functions ---
    val allRepairs: Flow<List<Repair>> = repairDao.getAllRepairsOrderedByDate()
    fun getStatsForPeriod(startDate: Long, endDate: Long): Flow<DashboardStats?> = repairDao.getStats(startDate, endDate)
    fun searchRepairs(searchQuery: String): Flow<List<Repair>> = repairDao.searchDatabase(searchQuery)
    fun getRepairById(id: Long): Flow<Repair?> = repairDao.getRepairById(id)
    suspend fun insert(repair: Repair) = repairDao.insert(repair)
    suspend fun update(repair: Repair) = repairDao.update(repair)
    suspend fun delete(repair: Repair) = repairDao.delete(repair)
    suspend fun getAllRepairsForBackup(): List<Repair> = repairDao.getAllRepairsForBackup()

    // --- Order Functions ---
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    fun getOrderStatsForPeriod(startDate: Long, endDate: Long): Flow<OrderDashboardStats?> = orderDao.getOrderStats(startDate, endDate)
    fun getOrderById(id: Long): Flow<Order?> = orderDao.getOrderById(id)
    fun searchOrders(query: String): Flow<List<Order>> = orderDao.searchOrders(query)
    suspend fun insertOrder(order: Order) = orderDao.insert(order)
    suspend fun updateOrder(order: Order) = orderDao.update(order)
    suspend fun deleteOrder(order: Order) = orderDao.delete(order)


    // --- Combined Backup/Restore ---
    @Transaction
    suspend fun restoreBackup(repairs: List<Repair>, orders: List<Order>) {
        repairDao.clearAll()
        orderDao.clearAll()
        repairs.forEach { repairDao.insert(it) }
        orders.forEach { orderDao.insert(it) }
    }
    suspend fun getAllOrdersForBackup(): List<Order> {
        return orderDao.getAllOrdersForBackup()
    }
}