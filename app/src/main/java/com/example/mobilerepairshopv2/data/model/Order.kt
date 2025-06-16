package com.example.mobilerepairshopv2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// This data class defines the structure of our new 'orders_table'.
@Entity(tableName = "orders_table")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    var customerName: String?,
    var customerContact: String,
    var imagePath: String?,
    var description: String?,
    var totalCost: Double,
    var advanceTaken: Double,
    var status: String,
    val dateAdded: Long,
    var dateCompleted: Long?
)
