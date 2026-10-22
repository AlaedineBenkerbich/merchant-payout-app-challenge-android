package com.example.androidinterview.domain.security

import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Money

/**
 * Decides which payouts must be confirmed with biometrics. The threshold is expressed in the minor
 * unit of the payout's own currency (100,000 = £1,000.00 or €1,000.00): no exchange rate is applied.
 */
class BiometricPolicy(private val thresholdMinorUnits: Long = DEFAULT_THRESHOLD_MINOR_UNITS) {

    /** Only payouts strictly above the threshold need authentication. */
    fun requiresAuthentication(amount: Money): Boolean = amount.minorUnits > thresholdMinorUnits

    fun threshold(currency: Currency): Money = Money(thresholdMinorUnits, currency)

    companion object {
        const val DEFAULT_THRESHOLD_MINOR_UNITS: Long = 100_000
    }
}
