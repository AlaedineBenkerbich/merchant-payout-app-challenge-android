package com.example.androidinterview.ui.common

import com.example.androidinterview.domain.model.Money
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Amounts and dates follow UK conventions whatever the device language: the account is a UK
 * merchant account and its currency symbols, separators and date style should look the same
 * for everyone who reads it.
 */
object MoneyFormatter {

    /** Formats with the currency symbol, e.g. `£5,000.00`, or `-£979.74` for an outflow. */
    fun format(money: Money, locale: Locale = Locale.UK): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = java.util.Currency.getInstance(money.currency.name)
        return format.format(money.toDecimal())
    }
}

object ActivityDateFormatter {

    private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK)

    fun format(date: LocalDate): String = formatter.format(date)

    /** Formats the local calendar day the instant falls on, e.g. `23 Jan 2026`. */
    fun format(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        format(instant.atZone(zone).toLocalDate())
}
