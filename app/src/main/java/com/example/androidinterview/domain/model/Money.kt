package com.example.androidinterview.domain.model

import java.math.BigDecimal

/**
 * An amount held in the minor unit of its currency (pence, cents), so that no floating point
 * arithmetic is ever involved. Outflows are negative.
 */
data class Money(val minorUnits: Long, val currency: Currency) {

    val isNegative: Boolean get() = minorUnits < 0

    fun toDecimal(): BigDecimal = BigDecimal.valueOf(minorUnits, currency.fractionDigits)
}
