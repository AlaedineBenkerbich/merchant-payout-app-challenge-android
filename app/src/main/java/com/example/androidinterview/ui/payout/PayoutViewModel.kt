package com.example.androidinterview.ui.payout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.domain.security.BiometricPolicy
import com.example.androidinterview.domain.usecase.SubmitPayoutUseCase
import com.example.androidinterview.domain.usecase.TrackPayoutStatusUseCase
import com.example.androidinterview.domain.usecase.ValidatePayoutDraftUseCase
import com.example.androidinterview.domain.validation.PayoutDraft
import com.example.androidinterview.domain.validation.ValidatedPayout
import com.example.androidinterview.ui.payout.biometric.BiometricResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the payout flow: form, confirmation, optional biometric check, submission and follow-up.
 *
 * The form values live in [SavedStateHandle] so they survive process death; the phase is not saved,
 * so after the process is killed the merchant lands back on a pre-filled form rather than in the
 * middle of a submission that may or may not have reached the server.
 */
class PayoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val validateDraft: ValidatePayoutDraftUseCase,
    private val biometricPolicy: BiometricPolicy,
    private val submitPayout: SubmitPayoutUseCase,
    private val trackPayoutStatus: TrackPayoutStatusUseCase,
) : ViewModel() {

    private val phase = MutableStateFlow<PayoutPhase>(PayoutPhase.Editing)
    private var submissionJob: Job? = null

    val uiState: StateFlow<PayoutUiState> = combine(
        savedStateHandle.getStateFlow(KEY_AMOUNT, ""),
        savedStateHandle.getStateFlow(KEY_CURRENCY, Currency.GBP.name),
        savedStateHandle.getStateFlow(KEY_IBAN, ""),
        phase,
    ) { amount, currency, iban, currentPhase ->
        buildUiState(PayoutDraft(amount, currencyOf(currency), iban), currentPhase)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = buildUiState(currentDraft(), PayoutPhase.Editing),
    )

    fun onAmountChanged(input: String) {
        savedStateHandle[KEY_AMOUNT] = sanitizeAmountInput(input)
    }

    fun onCurrencySelected(currency: Currency) {
        savedStateHandle[KEY_CURRENCY] = currency.name
    }

    fun onIbanChanged(input: String) {
        savedStateHandle[KEY_IBAN] = input.filterNot(Char::isWhitespace).uppercase()
    }

    fun onConfirmClicked() {
        if (phase.value != PayoutPhase.Editing) return
        val payout = validateDraft(currentDraft()).validatedPayout ?: return
        phase.value = PayoutPhase.Confirming(payout)
    }

    fun onConfirmationDismissed() {
        if (phase.value is PayoutPhase.Confirming) phase.value = PayoutPhase.Editing
    }

    fun onConfirmationAccepted() {
        val confirming = phase.value as? PayoutPhase.Confirming ?: return
        val payout = confirming.payout
        if (biometricPolicy.requiresAuthentication(payout.amount)) {
            phase.value = PayoutPhase.Authenticating(payout)
        } else {
            submit(payout)
        }
    }

    fun onBiometricResult(result: BiometricResult) {
        val authenticating = phase.value as? PayoutPhase.Authenticating ?: return
        val payout = authenticating.payout
        when (result) {
            BiometricResult.Success -> submit(payout)
            BiometricResult.Cancelled -> fail(PayoutFailure.BiometricCancelled)
            BiometricResult.NotEnrolled -> fail(
                PayoutFailure.BiometricNotSetUp(biometricPolicy.threshold(payout.amount.currency)),
            )
            BiometricResult.LockedOut -> fail(PayoutFailure.BiometricLockedOut)
            BiometricResult.Unavailable -> fail(PayoutFailure.BiometricUnavailable)
        }
    }

    /** Goes back to the form, with what the merchant typed still in place. */
    fun onTryAgain() {
        if (phase.value is PayoutPhase.Failed) phase.value = PayoutPhase.Editing
    }

    fun onCreateAnotherPayout() {
        submissionJob?.cancel()
        clearDraft()
        phase.value = PayoutPhase.Editing
    }

    /** Resumes following a payout whose status updates had stopped. */
    fun onCheckPayoutStatusAgain() {
        val succeeded = phase.value as? PayoutPhase.Succeeded ?: return
        if (!succeeded.isStatusStale) return
        submissionJob?.cancel()
        submissionJob = viewModelScope.launch { followUp(succeeded.payout) }
    }

    private fun submit(payout: ValidatedPayout) {
        phase.value = PayoutPhase.Submitting(payout)
        submissionJob?.cancel()
        submissionJob = viewModelScope.launch {
            when (val outcome = submitPayout(payout)) {
                is Outcome.Success -> {
                    // The payout exists now: a form restored after process death must not
                    // be one tap away from sending the same money again.
                    clearDraft()
                    followUp(outcome.value)
                }
                is Outcome.Failure -> fail(outcome.error.toFailure())
            }
        }
    }

    /** Shows the payout as soon as it is created, then keeps its status up to date. */
    private suspend fun followUp(created: Payout) {
        var latest = created
        phase.value = PayoutPhase.Succeeded(latest)
        trackPayoutStatus(created).collect { refreshed ->
            latest = refreshed
            phase.value = if (refreshed.status == PayoutStatus.FAILED) {
                PayoutPhase.Failed(PayoutFailure.ProcessingFailed)
            } else {
                PayoutPhase.Succeeded(refreshed)
            }
        }
        // Tracking ends without a final status when it has given up; say so instead of spinning forever.
        if (!latest.status.isFinal) phase.value = PayoutPhase.Succeeded(latest, isStatusStale = true)
    }

    private fun clearDraft() {
        savedStateHandle[KEY_AMOUNT] = ""
        savedStateHandle[KEY_IBAN] = ""
    }

    private fun fail(failure: PayoutFailure) {
        phase.value = PayoutPhase.Failed(failure)
    }

    private fun currentDraft() = PayoutDraft(
        amountInput = savedStateHandle[KEY_AMOUNT] ?: "",
        currency = currencyOf(savedStateHandle[KEY_CURRENCY] ?: Currency.GBP.name),
        ibanInput = savedStateHandle[KEY_IBAN] ?: "",
    )

    private fun buildUiState(draft: PayoutDraft, phase: PayoutPhase) = PayoutUiState(
        draft = draft,
        validation = validateDraft(draft),
        phase = phase,
    )

    private fun currencyOf(name: String): Currency = Currency.entries.firstOrNull { it.name == name } ?: Currency.GBP

    private fun DomainError.toFailure(): PayoutFailure = when (this) {
        DomainError.NoConnection -> PayoutFailure.NoConnection
        DomainError.ServiceUnavailable -> PayoutFailure.ServiceUnavailable
        DomainError.InsufficientFunds -> PayoutFailure.InsufficientFunds
        is DomainError.Rejected -> PayoutFailure.Rejected(message)
        DomainError.Unexpected -> PayoutFailure.Unexpected
    }

    companion object {
        internal const val KEY_AMOUNT = "payout_amount"
        internal const val KEY_CURRENCY = "payout_currency"
        internal const val KEY_IBAN = "payout_iban"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
