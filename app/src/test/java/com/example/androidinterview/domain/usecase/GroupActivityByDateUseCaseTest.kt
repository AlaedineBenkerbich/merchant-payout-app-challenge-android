package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.ActivityDay
import com.example.androidinterview.testing.activityItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupActivityByDateUseCaseTest {

    private val london = ZoneId.of("Europe/London")
    private val now = ZonedDateTime.of(2026, 10, 20, 12, 0, 0, 0, london).toInstant()

    private fun useCase(zone: ZoneId = london, at: Instant = now) =
        GroupActivityByDateUseCase { Clock.fixed(at, zone) }

    private fun localItem(id: String, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        activityItem(id = id, occurredAt = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, london).toInstant())

    @Test
    fun `should label groups today and yesterday when items happened on those local days`() {
        // Given
        val items = listOf(
            localItem("a", 2026, 10, 20, 9),
            localItem("b", 2026, 10, 19, 23, 30),
            localItem("c", 2025, 5, 18, 10),
        )

        // When
        val groups = useCase()(items)

        // Then
        assertEquals(
            listOf(ActivityDay.Today, ActivityDay.Yesterday, ActivityDay.On(LocalDate.of(2025, 5, 18))),
            groups.map { it.day },
        )
    }

    @Test
    fun `should keep the order of items when they belong to the same day`() {
        // Given
        val items = listOf(
            localItem("first", 2026, 10, 20, 11),
            localItem("second", 2026, 10, 20, 10),
            localItem("third", 2026, 10, 20, 9),
        )

        // When
        val groups = useCase()(items)

        // Then
        assertEquals(listOf("first", "second", "third"), groups.single().items.map { it.id })
    }

    @Test
    fun `should merge items of the same day when they come from different pages`() {
        // Given
        val firstPage = listOf(localItem("a", 2026, 10, 19, 20), localItem("b", 2026, 10, 19, 18))
        val secondPage = listOf(localItem("c", 2026, 10, 19, 9), localItem("d", 2026, 10, 18, 9))

        // When
        val groups = useCase()(firstPage + secondPage)

        // Then
        assertEquals(listOf("a", "b", "c"), groups.first().items.map { it.id })
        assertEquals(2, groups.size)
    }

    @Test
    fun `should use the local day rather than the utc day when the zone is ahead of utc`() {
        // Given: 23:30 UTC on 19 October is 01:30 on 20 October in Paris (UTC+2).
        val paris = ZoneId.of("Europe/Paris")
        val item = activityItem(occurredAt = Instant.parse("2026-10-19T23:30:00Z"))

        // When
        val groups = useCase(zone = paris, at = Instant.parse("2026-10-20T10:00:00Z"))(listOf(item))

        // Then
        assertEquals(ActivityDay.Today, groups.single().day)
    }

    @Test
    fun `should order groups from the most recent day when items arrive unsorted`() {
        // Given
        val items = listOf(
            localItem("old", 2026, 9, 1, 10),
            localItem("recent", 2026, 10, 18, 10),
        )

        // When
        val groups = useCase()(items)

        // Then
        assertEquals(listOf("recent", "old"), groups.map { it.items.single().id })
    }

    @Test
    fun `should return no groups when there is no activity`() {
        // Given
        val items = emptyList<com.example.androidinterview.domain.model.ActivityItem>()

        // When
        val groups = useCase()(items)

        // Then
        assertEquals(emptyList(), groups)
    }
}
