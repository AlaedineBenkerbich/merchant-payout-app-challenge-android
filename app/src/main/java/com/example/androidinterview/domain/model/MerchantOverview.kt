package com.example.androidinterview.domain.model

data class MerchantOverview(
    val availableBalance: Money,
    val pendingBalance: Money,
    val recentActivity: List<ActivityItem>,
)
