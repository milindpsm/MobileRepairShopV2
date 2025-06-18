package com.example.mobilerepairshopv2.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilerepairshopv2.data.RepairRepository
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Order
import com.example.mobilerepairshopv2.data.model.OrderDashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.launch

class RepairViewModel(private val repository: RepairRepository) : ViewModel() {

    // --- Repair LiveData ---
    val allRepairs: LiveData<List<Repair>> = repository.allRepairs.asLiveData()

    // --- Order LiveData ---
    val allOrders: LiveData<List<Order>> = repository.allOrders.asLiveData()

    // --- Repair functions ---
    fun insert(repair: Repair) = viewModelScope.launch { repository.insert(repair) }
    fun update(repair: Repair) = viewModelScope.launch { repository.update(repair) }
    fun delete(repair: Repair) = viewModelScope.launch { repository.delete(repair) }
    fun getRepairById(id: Long): LiveData<Repair?> = repository.getRepairById(id).asLiveData()
    fun searchRepairs(query: String): LiveData<List<Repair>> = repository.searchRepairs(query).asLiveData()
    fun getStatsForPeriod(startDate: Long, endDate: Long): LiveData<DashboardStats?> = repository.getStatsForPeriod(startDate, endDate).asLiveData()

    // --- Order functions ---
    fun insertOrder(order: Order) = viewModelScope.launch { repository.insertOrder(order) }
    fun updateOrder(order: Order) = viewModelScope.launch { repository.updateOrder(order) }
    fun deleteOrder(order: Order) = viewModelScope.launch { repository.deleteOrder(order) }
    fun getOrderById(id: Long): LiveData<Order?> = repository.getOrderById(id).asLiveData()
    fun searchOrders(query: String): LiveData<List<Order>> = repository.searchOrders(query).asLiveData()
    fun getOrderStatsForPeriod(startDate: Long, endDate: Long): LiveData<OrderDashboardStats?> = repository.getOrderStatsForPeriod(startDate, endDate).asLiveData()

    // --- Backup/Restore ---
    suspend fun getRepairsForBackup(): List<Repair> = repository.getAllRepairsForBackup()
    suspend fun getAllOrdersForBackup(): List<Order> = repository.getAllOrdersForBackup()
    fun restoreBackup(repairs: List<Repair>, orders: List<Order>) = viewModelScope.launch {
        repository.restoreBackup(repairs, orders)
    }
}

class RepairViewModelFactory(private val repository: RepairRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RepairViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RepairViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    suspend fun getAllOrdersForBackup(): List<Order> {
        return repository.getAllOrdersForBackup()
    }
}