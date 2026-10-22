package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.repository.DeviceIdentityRepository
import com.example.androidinterview.domain.repository.PayoutRepository
import com.example.androidinterview.domain.validation.ValidatedPayout

/** Attaches the device identity to a validated payout and sends it. */
class SubmitPayoutUseCase(
    private val deviceIdentity: DeviceIdentityRepository,
    private val payouts: PayoutRepository,
) {

    suspend operator fun invoke(payout: ValidatedPayout): Outcome<Payout> =
        when (val device = deviceIdentity.getDeviceId()) {
            is Outcome.Failure -> device
            is Outcome.Success -> payouts.createPayout(
                PayoutRequest(amount = payout.amount, iban = payout.iban, deviceId = device.value),
            )
        }
}
