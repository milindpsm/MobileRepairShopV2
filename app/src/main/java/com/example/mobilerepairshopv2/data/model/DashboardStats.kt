package com.example.mobilerepairshopv2.data.model

/**
 * This is a simple data holder class, not an entity.
 * It's updated to hold more specific values from our new, complex stats query.
 */
data class DashboardStats(
    val inCount: Int,
    val outCount: Int,
    // Sum of totalCost for all jobs in the period
    val estimatedRevenue: Double?,
    // Sum of advanceTaken for 'In' and 'Pending' jobs
    val advanceFromPending: Double?,
    // Sum of totalCost for 'Out' jobs (since the full amount is collected)
    val revenueFromOut: Double?,
    // Sum of remaining due for 'In' and 'Pending' jobs
    val upcomingRevenue: Double?
)
