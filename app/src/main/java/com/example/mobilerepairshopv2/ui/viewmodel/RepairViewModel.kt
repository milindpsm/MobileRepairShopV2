package com.example.mobilerepairshopv2.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mobilerepairshopv2.data.RepairRepository
import com.example.mobilerepairshopv2.data.model.DashboardStats
import com.example.mobilerepairshopv2.data.model.Repair
import kotlinx.coroutines.launch

class RepairViewModel(private val repository: RepairRepository) : ViewModel() {

    val allRepairs: LiveData<List<Repair>> = repository.allRepairs.asLiveData()
    val pendingCount: LiveData<Int> = repository.pendingCount.asLiveData()

    fun insert(repair: Repair) = viewModelScope.launch {
        repository.insert(repair)
    }

    fun update(repair: Repair) = viewModelScope.launch {
        repository.update(repair)
    }

    fun delete(repair: Repair) = viewModelScope.launch {
        repository.delete(repair)
    }

    fun getRepairById(id: Long): LiveData<Repair?> {
        return repository.getRepairById(id).asLiveData()
    }

    fun searchRepairs(query: String): LiveData<List<Repair>> {
        return repository.searchRepairs(query).asLiveData()
    }

    // --- THIS IS THE CORRECTED FUNCTION ---
    fun getStatsForPeriod(startDate: Long, endDate: Long): LiveData<DashboardStats?> {
        // We add .asLiveData() to correctly convert the Flow to LiveData
        return repository.getStatsForPeriod(startDate, endDate).asLiveData()
    }

    suspend fun getRepairsForBackup(): List<Repair> {
        return repository.getAllRepairsForBackup()
    }

    fun restoreBackup(repairs: List<Repair>) = viewModelScope.launch {
        repository.restoreFromBackup(repairs)
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
}
