package com.example.mobilerepairshopv2.data.model

data class DashboardStats(
    val inCount: Int,
    val outCount: Int,
    val pendingCount: Int, // <-- NEWLY ADDED
    val estimatedRevenue: Double?,
    val advanceFromPending: Double?,
    val revenueFromOut: Double?,
    val upcomingRevenue: Double?
)
