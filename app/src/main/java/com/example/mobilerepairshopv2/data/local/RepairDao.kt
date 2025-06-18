package com.example.mobilerepairshopv2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {
    @Insert
    suspend fun insert(repair: Repair)

    @Update
    suspend fun update(repair: Repair)

    @Delete
    suspend fun delete(repair: Repair)

    @Query("SELECT * FROM repairs_table WHERE id = :id")
    fun getRepairById(id: Long): Flow<Repair?>

    @Query("SELECT * FROM repairs_table ORDER BY dateAdded DESC")
    fun getAllRepairsOrderedByDate(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs_table WHERE customerName LIKE '%' || :searchQuery || '%' OR customerContact LIKE '%' || :searchQuery || '%' ORDER BY dateAdded DESC")
    fun searchDatabase(searchQuery: String): Flow<List<Repair>>

    // --- THIS IS THE FINAL, CORRECTED QUERY ---
    @Query("""
        SELECT
            SUM(CASE WHEN status = 'In' THEN 1 ELSE 0 END) as inCount,
            SUM(CASE WHEN status = 'Out' THEN 1 ELSE 0 END) as outCount,
            SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) as pendingCount,
            SUM(totalCost) as estimatedRevenue,
            SUM(CASE WHEN status != 'Out' THEN advanceTaken ELSE 0 END) as advanceFromPending,
            SUM(CASE WHEN status = 'Out' THEN totalCost ELSE 0 END) as revenueFromOut,
            SUM(CASE WHEN status != 'Out' THEN (totalCost - advanceTaken) ELSE 0 END) as upcomingRevenue
        FROM repairs_table
        WHERE dateAdded BETWEEN :startDate AND :endDate
    """)
    fun getStats(startDate: Long, endDate: Long): Flow<DashboardStats?>

    @Query("SELECT * FROM repairs_table ORDER BY dateAdded DESC")
    suspend fun getAllRepairsForBackup(): List<Repair>

    @Query("DELETE FROM repairs_table")
    suspend fun clearAll()
}