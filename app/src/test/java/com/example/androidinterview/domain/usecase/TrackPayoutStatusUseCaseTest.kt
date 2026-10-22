package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.testing.FakePayoutRepository
import com.example.androidinterview.testing.payout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class TrackPayoutStatusUseCaseTest {

    private val payouts = FakePayoutRepository()
    private val trackStatus = TrackPayoutStatusUseCase(payouts, pollInterval = 2.seconds, maxPolls = 3)

    @Test
    fun `should emit each status until the payout completes when polling succeeds`() = runTest {
        // Given
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        val statuses = trackStatus(payout(status = PayoutStatus.PENDING)).toList().map { it.status }

        // Then
        assertEquals(listOf(PayoutStatus.PENDING, PayoutStatus.PROCESSING, PayoutStatus.COMPLETED), statuses)
        assertEquals(2, payouts.getRequests)
    }

    @Test
    fun `should keep following the payout while its status is one this version does not know`() = runTest {
        // Given
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.UNKNOWN))
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        val statuses = trackStatus(payout(status = PayoutStatus.PENDING)).toList().map { it.status }

        // Then
        assertEquals(listOf(PayoutStatus.PENDING, PayoutStatus.UNKNOWN, PayoutStatus.COMPLETED), statuses)
    }

    @Test
    fun `should not poll when the payout is already final`() = runTest {
        // Given
        val completed = payout(status = PayoutStatus.COMPLETED)

        // When
        val emitted = trackStatus(completed).toList()

        // Then
        assertEquals(listOf(completed), emitted)
        assertEquals(0, payouts.getRequests)
    }

    @Test
    fun `should stop when the payout fails`() = runTest {
        // Given
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.FAILED))

        // When
        val statuses = trackStatus(payout(status = PayoutStatus.PENDING)).toList().map { it.status }

        // Then
        assertEquals(listOf(PayoutStatus.PENDING, PayoutStatus.FAILED), statuses)
    }

    @Test
    fun `should keep polling when a status request fails`() = runTest {
        // Given
        payouts.getOutcomes += Outcome.Failure(DomainError.NoConnection)
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        val statuses = trackStatus(payout(status = PayoutStatus.PENDING)).toList().map { it.status }

        // Then
        assertEquals(listOf(PayoutStatus.PENDING, PayoutStatus.COMPLETED), statuses)
    }

    @Test
    fun `should give up when the payout is still not final after the maximum number of polls`() = runTest {
        // Given
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))

        // When
        val statuses = trackStatus(payout(status = PayoutStatus.PENDING)).toList().map { it.status }

        // Then
        assertEquals(
            listOf(PayoutStatus.PENDING, PayoutStatus.PROCESSING, PayoutStatus.PROCESSING, PayoutStatus.PROCESSING),
            statuses,
        )
        assertEquals(3, payouts.getRequests)
    }
}
