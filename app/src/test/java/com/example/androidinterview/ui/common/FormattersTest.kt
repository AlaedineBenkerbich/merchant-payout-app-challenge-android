package com.example.androidinterview.ui.common

import com.example.androidinterview.testing.eur
import com.example.androidinterview.testing.gbp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {

    @Test
    fun `should format pounds with symbol thousands separator and two decimals`() {
        // Given
        val balance = gbp(500_000)

        // When
        val formatted = MoneyFormatter.format(balance)

        // Then
        assertEquals("£5,000.00", formatted)
    }

    @Test
    fun `should format euros with the euro symbol`() {
        // Given
        val amount = eur(2_500)

        // When
        val formatted = MoneyFormatter.format(amount)

        // Then
        assertEquals("€25.00", formatted)
    }

    @Test
    fun `should put the minus sign before the currency symbol when the amount is an outflow`() {
        // Given
        val outflow = gbp(-97_974)

        // When
        val formatted = MoneyFormatter.format(outflow)

        // Then
        assertEquals("-£979.74", formatted)
    }

    @Test
    fun `should keep pence when the amount has fewer than two digits of minor units`() {
        // Given
        val onePenny = gbp(1)

        // When
        val formatted = MoneyFormatter.format(onePenny)

        // Then
        assertEquals("£0.01", formatted)
    }

    @Test
    fun `should format the same way when the device language is not english`() {
        // Given
        val previousDefault = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)

        try {
            // When
            val formatted = MoneyFormatter.format(gbp(500_000))

            // Then
            assertEquals("£5,000.00", formatted)
        } finally {
            Locale.setDefault(previousDefault)
        }
    }

    @Test
    fun `should format a date as day abbreviated month and year`() {
        // Given
        val date = LocalDate.of(2026, 1, 23)

        // When
        val formatted = ActivityDateFormatter.format(date)

        // Then
        assertEquals("23 Jan 2026", formatted)
    }

    @Test
    fun `should show the local day when an instant is formatted in a zone ahead of utc`() {
        // Given: 23:30 UTC on 23 January is already 24 January in Paris.
        val instant = Instant.parse("2026-01-23T23:30:00Z")

        // When
        val formatted = ActivityDateFormatter.format(instant, ZoneId.of("Europe/Paris"))

        // Then
        assertEquals("24 Jan 2026", formatted)
    }
}
