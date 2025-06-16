package com.example.mobilerepairshopv2.data.model

/**
 * A simple data class to hold the statistics for the Orders dashboard.
 */
data class OrderDashboardStats(
    val inCount: Int,
    val outCount: Int,
    val pendingCount: Int
)