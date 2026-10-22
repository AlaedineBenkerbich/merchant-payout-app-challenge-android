package com.example.androidinterview.domain.model

/** Currencies a merchant can pay out in, with the number of decimals of their minor unit. */
enum class Currency(val fractionDigits: Int) {
    GBP(fractionDigits = 2),
    EUR(fractionDigits = 2),
}
