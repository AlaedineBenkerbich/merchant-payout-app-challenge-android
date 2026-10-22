package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.ActivityDay
import com.example.androidinterview.domain.model.ActivityGroup
import com.example.androidinterview.domain.model.ActivityItem
import java.time.Clock
import java.time.LocalDate

/**
 * Groups activity by the merchant's local calendar day. The clock is read on every call so a time
 * zone change while the app is running is picked up, and so tests can pin "now" and the zone.
 */
class GroupActivityByDateUseCase(private val clock: () -> Clock = Clock::systemDefaultZone) {

    operator fun invoke(items: List<ActivityItem>): List<ActivityGroup> {
        val currentClock = clock()
        val zone = currentClock.zone
        val today = LocalDate.now(currentClock)

        return items
            .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
            .entries
            .sortedByDescending { (date, _) -> date }
            .map { (date, itemsOfTheDay) -> ActivityGroup(dayOf(date, today), itemsOfTheDay) }
    }

    private fun dayOf(date: LocalDate, today: LocalDate): ActivityDay = when (date) {
        today -> ActivityDay.Today
        today.minusDays(1) -> ActivityDay.Yesterday
        else -> ActivityDay.On(date)
    }
}
