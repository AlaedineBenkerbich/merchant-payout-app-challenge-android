package com.example.androidinterview.domain.model

import java.time.Instant

enum class PayoutStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,

    /** A status the API introduced after this version of the app was written: not final, so it keeps being followed. */
    UNKNOWN;

    /** A final status never changes again, so there is no point polling for it. */
    val isFinal: Boolean get() = this == COMPLETED || this == FAILED
}

data class Payout(
    val id: String,
    val status: PayoutStatus,
    val amount: Money,
    val iban: String,
    val createdAt: Instant,
)

/** What the API needs to create a payout; the device id is attached just before sending. */
data class PayoutRequest(
    val amount: Money,
    val iban: Iban,
    val deviceId: String,
)
