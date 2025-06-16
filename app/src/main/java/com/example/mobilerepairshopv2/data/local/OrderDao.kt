package com.example.mobilerepairshopv2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.OrderDashboardStats
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    // Basic Operations
    @Insert
    suspend fun insert(order: Order)

    @Update
    suspend fun update(order: Order)

    @Delete
    suspend fun delete(order: Order)

    // Query to get a single order by its ID
    @Query("SELECT * FROM orders_table WHERE id = :id")
    fun getOrderById(id: Long): Flow<Order?>

    // Query to get all orders, newest first
    @Query("SELECT * FROM orders_table ORDER BY dateAdded DESC")
    fun getAllOrders(): Flow<List<Order>>

    // Query for searching orders
    @Query("SELECT * FROM orders_table WHERE customerName LIKE '%' || :searchQuery || '%' OR customerContact LIKE '%' || :searchQuery || '%' ORDER BY dateAdded DESC")
    fun searchOrders(searchQuery: String): Flow<List<Order>>

    // Query for the simple stats on the orders dashboard
    // This is the updated function inside OrderDao.kt
    @Query("""
    SELECT
        (SELECT COUNT(*) FROM orders_table WHERE dateAdded BETWEEN :startDate AND :endDate) as inCount,
        (SELECT COUNT(*) FROM orders_table WHERE status = 'Out' AND dateCompleted BETWEEN :startDate AND :endDate) as outCount,
        (SELECT COUNT(*) FROM orders_table WHERE status = 'Pending' AND dateAdded BETWEEN :startDate AND :endDate AND status != 'Out') as pendingCount
""")
    fun getOrderStats(startDate: Long, endDate: Long): Flow<OrderDashboardStats?>
}
