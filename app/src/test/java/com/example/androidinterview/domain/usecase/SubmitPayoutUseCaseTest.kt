package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.testing.FakeDeviceIdentityRepository
import com.example.androidinterview.testing.FakePayoutRepository
import com.example.androidinterview.testing.gbp
import com.example.androidinterview.testing.payout
import com.example.androidinterview.testing.validatedPayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SubmitPayoutUseCaseTest {

    private val payouts = FakePayoutRepository()
    private val deviceIdentity = FakeDeviceIdentityRepository(Outcome.Success("device_123"))
    private val submitPayout = SubmitPayoutUseCase(deviceIdentity, payouts)

    @Test
    fun `should send payout with the device identity when it is available`() = runTest {
        // Given
        val validated = validatedPayout(gbp(40_000))
        payouts.createOutcome = Outcome.Success(payout())

        // When
        val outcome = submitPayout(validated)

        // Then
        assertEquals(Outcome.Success(payout()), outcome)
        assertEquals(
            PayoutRequest(amount = gbp(40_000), iban = validated.iban, deviceId = "device_123"),
            payouts.createdRequests.single(),
        )
    }

    @Test
    fun `should not create the payout when the device identity cannot be obtained`() = runTest {
        // Given
        deviceIdentity.outcome = Outcome.Failure(DomainError.NoConnection)

        // When
        val outcome = submitPayout(validatedPayout())

        // Then
        assertEquals(Outcome.Failure(DomainError.NoConnection), outcome)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should return the failure when the api refuses the payout`() = runTest {
        // Given
        payouts.createOutcome = Outcome.Failure(DomainError.InsufficientFunds)

        // When
        val outcome = submitPayout(validatedPayout())

        // Then
        assertEquals(Outcome.Failure(DomainError.InsufficientFunds), outcome)
    }
}
