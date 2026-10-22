package com.example.androidinterview.ui.payout

import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.validation.PayoutDraft
import com.example.androidinterview.domain.validation.PayoutDraftValidation
import com.example.androidinterview.domain.validation.ValidatedPayout

data class PayoutUiState(
    val draft: PayoutDraft,
    val validation: PayoutDraftValidation,
    val phase: PayoutPhase,
) {
    /** The "Confirm" button is only enabled while editing a draft that can be submitted. */
    val canConfirm: Boolean get() = phase is PayoutPhase.Editing && validation.validatedPayout != null
}

/** Where the merchant is in the payout flow. The form values are kept in every phase. */
sealed interface PayoutPhase {
    data object Editing : PayoutPhase
    data class Confirming(val payout: ValidatedPayout) : PayoutPhase
    data class Authenticating(val payout: ValidatedPayout) : PayoutPhase
    data class Submitting(val payout: ValidatedPayout) : PayoutPhase
    /**
     * The payout exists. [isStatusStale] means follow-up has stopped before the payout reached a
     * final status (it is taking long, or the network kept failing), so the screen must not keep
     * promising a result: it offers to check again instead.
     */
    data class Succeeded(val payout: Payout, val isStatusStale: Boolean = false) : PayoutPhase
    data class Failed(val failure: PayoutFailure) : PayoutPhase
}

sealed interface PayoutFailure {
    data object InsufficientFunds : PayoutFailure
    data object ServiceUnavailable : PayoutFailure
    data object NoConnection : PayoutFailure
    data class Rejected(val message: String?) : PayoutFailure
    data object Unexpected : PayoutFailure

    /** The payout was created but the bank reported that it could not be completed. */
    data object ProcessingFailed : PayoutFailure

    data object BiometricCancelled : PayoutFailure
    data class BiometricNotSetUp(val threshold: Money) : PayoutFailure
    data object BiometricLockedOut : PayoutFailure
    data object BiometricUnavailable : PayoutFailure
}
