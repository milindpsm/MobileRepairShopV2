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
    @Query("""
        SELECT
            IFNULL(SUM(CASE WHEN status = 'In' AND dateAdded BETWEEN :startDate AND :endDate THEN 1 ELSE 0 END), 0) as inCount,
            IFNULL(SUM(CASE WHEN status = 'Out' AND dateCompleted BETWEEN :startDate AND :endDate THEN 1 ELSE 0 END), 0) as outCount,
            IFNULL(SUM(CASE WHEN status = 'Pending' AND dateAdded BETWEEN :startDate AND :endDate THEN 1 ELSE 0 END), 0) as pendingCount
        FROM orders_table
    """)
    fun getOrderStats(startDate: Long, endDate: Long): Flow<OrderDashboardStats?>

    // One-time fetch for backup
    @Query("SELECT * FROM orders_table ORDER BY dateAdded DESC")
    suspend fun getAllOrdersForBackup(): List<Order>

    // Deletes all data for restore
    @Query("DELETE FROM orders_table")
    suspend fun clearAll()


}
