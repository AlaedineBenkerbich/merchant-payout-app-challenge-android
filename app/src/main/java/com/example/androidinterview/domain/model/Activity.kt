package com.example.androidinterview.domain.model

import java.time.Instant
import java.time.LocalDate

/** [OTHER] stands for a type the API introduced after this version of the app was written. */
enum class ActivityType { PAYOUT, DEPOSIT, REFUND, FEE, OTHER }

/** [UNKNOWN] stands for a status the API introduced after this version of the app was written. */
enum class ActivityStatus { COMPLETED, PENDING, PROCESSING, FAILED, UNKNOWN }

data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val amount: Money,
    val occurredAt: Instant,
    val description: String,
    val status: ActivityStatus,
)

/** One page of a cursor-paginated activity feed. */
data class ActivityPage(
    val items: List<ActivityItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

/** The local calendar day a group of activity items belongs to, relative to today. */
sealed interface ActivityDay {
    data object Today : ActivityDay
    data object Yesterday : ActivityDay
    data class On(val date: LocalDate) : ActivityDay
}

data class ActivityGroup(
    val day: ActivityDay,
    val items: List<ActivityItem>,
)
