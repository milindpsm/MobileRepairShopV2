package com.example.mobilerepairshopv2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repairs_table")
data class Repair(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- MODIFIED: Made customerName and description nullable ---
    var customerName: String?,
    var customerContact: String,
    var alternateContact: String?,
    var imeiNumber: String?,
    var description: String?,
    var imagePath: String?,
    var totalCost: Double,
    var advanceTaken: Double,
    var status: String,
    val dateAdded: Long,
    var dateCompleted: Long?
)
