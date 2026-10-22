package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.repository.PayoutRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A freshly created payout is usually still pending. This follows it until it reaches a final
 * status, so the merchant is only told "completed" once the API says so.
 */
class TrackPayoutStatusUseCase(
    private val payouts: PayoutRepository,
    private val pollInterval: Duration = 2.seconds,
    private val maxPolls: Int = 20,
) {

    /** Emits [initial], then every refreshed status, until the payout is final or polling gives up. */
    operator fun invoke(initial: Payout): Flow<Payout> = flow {
        var latest = initial
        emit(latest)

        var polls = 0
        while (!latest.status.isFinal && polls < maxPolls) {
            delay(pollInterval)
            polls++
            // A failed poll is transient: keep the last known status and try again.
            val outcome = payouts.getPayout(latest.id)
            if (outcome is Outcome.Success) {
                latest = outcome.value
                emit(latest)
            }
        }
    }
}
