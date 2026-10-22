package com.example.androidinterview.ui.payout

import androidx.lifecycle.SavedStateHandle
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.domain.security.BiometricPolicy
import com.example.androidinterview.domain.usecase.SubmitPayoutUseCase
import com.example.androidinterview.domain.usecase.TrackPayoutStatusUseCase
import com.example.androidinterview.domain.usecase.ValidatePayoutDraftUseCase
import com.example.androidinterview.domain.validation.AmountValidation
import com.example.androidinterview.domain.validation.PayoutDraft
import com.example.androidinterview.testing.FakeDeviceIdentityRepository
import com.example.androidinterview.testing.FakePayoutRepository
import com.example.androidinterview.testing.VALID_IBAN
import com.example.androidinterview.testing.eur
import com.example.androidinterview.testing.gbp
import com.example.androidinterview.testing.payout
import com.example.androidinterview.testing.validIban
import com.example.androidinterview.ui.payout.biometric.BiometricResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PayoutViewModelTest {

    private val payouts = FakePayoutRepository()
    private val deviceIdentity = FakeDeviceIdentityRepository(Outcome.Success("device_123"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the view model and keeps `uiState` hot, as the screen does while it is visible. */
    private fun TestScope.viewModel(savedState: SavedStateHandle = SavedStateHandle()): PayoutViewModel {
        val viewModel = PayoutViewModel(
            savedStateHandle = savedState,
            validateDraft = ValidatePayoutDraftUseCase(),
            biometricPolicy = BiometricPolicy(),
            submitPayout = SubmitPayoutUseCase(deviceIdentity, payouts),
            trackPayoutStatus = TrackPayoutStatusUseCase(payouts, pollInterval = 2.seconds, maxPolls = 5),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel
    }

    private fun PayoutViewModel.fillForm(amount: String = "400", iban: String = VALID_IBAN) {
        onAmountChanged(amount)
        onIbanChanged(iban)
    }

    private fun PayoutViewModel.confirm() {
        onConfirmClicked()
        onConfirmationAccepted()
    }

    private val PayoutViewModel.phase: PayoutPhase get() = uiState.value.phase

    // --- Form ---------------------------------------------------------------------------------

    @Test
    fun `should not allow confirmation when the form is empty`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.canConfirm)
    }

    @Test
    fun `should allow confirmation when amount and iban are valid`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.fillForm(amount = "400", iban = VALID_IBAN)

        // Then
        assertTrue(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `should not allow confirmation when the amount is zero`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.fillForm(amount = "0.00")

        // Then
        assertFalse(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `should not allow confirmation when the iban is invalid`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.fillForm(iban = "FR1212345123451234567A12310131231231231")

        // Then
        assertFalse(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `should normalise the iban when it is typed with spaces and lower case`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.onIbanChanged("gb29 nwbk 6016 1331 9268 19")

        // Then
        assertEquals(VALID_IBAN, viewModel.uiState.value.draft.ibanInput)
    }

    @Test
    fun `should drop what cannot be part of an amount when the amount is typed or pasted`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.onAmountChanged("£ 1a2")

        // Then
        assertEquals("12", viewModel.uiState.value.draft.amountInput)
    }

    @Test
    fun `should not allow confirmation and explain why when the amount is negative`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.fillForm(amount = "-500")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canConfirm)
        assertEquals(AmountValidation.NotPositive, state.validation.amount)
    }

    @Test
    fun `should pay the amount that was pasted when it is written with thousands separators`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "£5,000.00")

        // When
        viewModel.onConfirmClicked()

        // Then
        val phase = assertIs<PayoutPhase.Confirming>(viewModel.phase)
        assertEquals(gbp(500_000), phase.payout.amount)
    }

    @Test
    fun `should not allow confirmation when the amount is ambiguous`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.fillForm(amount = "1,2500")

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canConfirm)
        assertEquals(AmountValidation.NotANumber, state.validation.amount)
    }

    @Test
    fun `should restore the form when the view model is recreated from saved state`() = runTest {
        // Given
        val savedState = SavedStateHandle(
            mapOf(
                PayoutViewModel.KEY_AMOUNT to "400",
                PayoutViewModel.KEY_CURRENCY to Currency.EUR.name,
                PayoutViewModel.KEY_IBAN to VALID_IBAN,
            ),
        )

        // When
        val viewModel = viewModel(savedState)

        // Then
        assertEquals(PayoutDraft("400", Currency.EUR, VALID_IBAN), viewModel.uiState.value.draft)
    }

    // --- Confirmation -------------------------------------------------------------------------

    @Test
    fun `should ask for confirmation when the merchant confirms a valid form`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()

        // When
        viewModel.onConfirmClicked()

        // Then
        val phase = assertIs<PayoutPhase.Confirming>(viewModel.phase)
        assertEquals(gbp(40_000), phase.payout.amount)
    }

    @Test
    fun `should not ask for confirmation when the form is invalid`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.onConfirmClicked()

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
    }

    @Test
    fun `should go back to editing when the confirmation is dismissed`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        viewModel.onConfirmClicked()

        // When
        viewModel.onConfirmationDismissed()

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    // --- Submission ---------------------------------------------------------------------------

    @Test
    fun `should submit directly when the amount is below the biometric threshold`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "999.99")
        payouts.createOutcome = Outcome.Success(payout(amount = gbp(99_999)))
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.confirm()

        // Then
        assertEquals(
            PayoutRequest(amount = gbp(99_999), iban = validIban(), deviceId = "device_123"),
            payouts.createdRequests.single(),
        )
    }

    @Test
    fun `should submit directly when the amount equals the biometric threshold`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "1000")
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.confirm()

        // Then
        assertEquals(1, payouts.createdRequests.size)
    }

    @Test
    fun `should ask for biometrics before submitting when the amount is above the threshold`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "1000.01")

        // When
        viewModel.confirm()

        // Then
        assertIs<PayoutPhase.Authenticating>(viewModel.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should apply the biometric threshold in euros when paying out in euros`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.onCurrencySelected(Currency.EUR)
        viewModel.fillForm(amount = "1000.01")

        // When
        viewModel.confirm()

        // Then
        val phase = assertIs<PayoutPhase.Authenticating>(viewModel.phase)
        assertEquals(eur(100_001), phase.payout.amount)
    }

    @Test
    fun `should submit when biometric authentication succeeds`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "2000")
        viewModel.confirm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.onBiometricResult(BiometricResult.Success)

        // Then
        assertEquals(gbp(200_000), payouts.createdRequests.single().amount)
    }

    @Test
    fun `should abort the payout when biometric authentication is cancelled`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "2000")
        viewModel.confirm()

        // When
        viewModel.onBiometricResult(BiometricResult.Cancelled)

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.BiometricCancelled), viewModel.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should explain how to set up biometrics when none is enrolled`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "2000")
        viewModel.confirm()

        // When
        viewModel.onBiometricResult(BiometricResult.NotEnrolled)

        // Then
        assertEquals(
            PayoutPhase.Failed(PayoutFailure.BiometricNotSetUp(threshold = gbp(100_000))),
            viewModel.phase,
        )
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should abort the payout when biometrics are unavailable or locked`() = runTest {
        // Given
        val lockedOut = viewModel().apply { fillForm(amount = "2000"); confirm() }
        val unavailable = viewModel().apply { fillForm(amount = "2000"); confirm() }

        // When
        lockedOut.onBiometricResult(BiometricResult.LockedOut)
        unavailable.onBiometricResult(BiometricResult.Unavailable)

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.BiometricLockedOut), lockedOut.phase)
        assertEquals(PayoutPhase.Failed(PayoutFailure.BiometricUnavailable), unavailable.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should ignore a biometric result when no authentication was requested`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()

        // When
        viewModel.onBiometricResult(BiometricResult.Success)

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should send the payout only once when confirmation is accepted repeatedly`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.createGate = CompletableDeferred()
        viewModel.onConfirmClicked()

        // When
        viewModel.onConfirmationAccepted()
        viewModel.onConfirmationAccepted()
        viewModel.onConfirmClicked()

        // Then
        assertIs<PayoutPhase.Submitting>(viewModel.phase)
        assertEquals(1, payouts.createdRequests.size)
    }

    // --- Outcomes -----------------------------------------------------------------------------

    @Test
    fun `should report insufficient funds when the api refuses the payout for that reason`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "888.88")
        payouts.createOutcome = Outcome.Failure(DomainError.InsufficientFunds)

        // When
        viewModel.confirm()

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.InsufficientFunds), viewModel.phase)
    }

    @Test
    fun `should report service unavailable when the api is down`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "999.99")
        payouts.createOutcome = Outcome.Failure(DomainError.ServiceUnavailable)

        // When
        viewModel.confirm()

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.ServiceUnavailable), viewModel.phase)
    }

    @Test
    fun `should report a network failure when the device cannot reach the server`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        deviceIdentity.outcome = Outcome.Failure(DomainError.NoConnection)

        // When
        viewModel.confirm()

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.NoConnection), viewModel.phase)
        assertTrue(payouts.createdRequests.isEmpty())
    }

    @Test
    fun `should keep the form values when the merchant tries again after a failure`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm(amount = "888.88")
        payouts.createOutcome = Outcome.Failure(DomainError.InsufficientFunds)
        viewModel.confirm()

        // When
        viewModel.onTryAgain()

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
        assertEquals(PayoutDraft("888.88", Currency.GBP, VALID_IBAN), viewModel.uiState.value.draft)
        assertTrue(viewModel.uiState.value.canConfirm)
    }

    @Test
    fun `should show the payout right away and follow it until it completes`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.createOutcome = Outcome.Success(payout(status = PayoutStatus.PENDING))
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.confirm()

        // Then
        assertEquals(PayoutPhase.Succeeded(payout(status = PayoutStatus.PENDING)), viewModel.phase)

        advanceTimeBy(2.seconds + 1.seconds / 10)
        assertEquals(PayoutPhase.Succeeded(payout(status = PayoutStatus.PROCESSING)), viewModel.phase)

        advanceUntilIdle()
        assertEquals(PayoutPhase.Succeeded(payout(status = PayoutStatus.COMPLETED)), viewModel.phase)
    }

    @Test
    fun `should clear the saved form as soon as the payout has been created`() = runTest {
        // Given
        val savedState = SavedStateHandle()
        val viewModel = viewModel(savedState)
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.confirm()

        // Then: a form restored after process death must not invite sending the same payout again.
        assertEquals("", savedState[PayoutViewModel.KEY_AMOUNT])
        assertEquals("", savedState[PayoutViewModel.KEY_IBAN])
    }

    @Test
    fun `should keep the saved form when the payout could not be created`() = runTest {
        // Given
        val savedState = SavedStateHandle()
        val viewModel = viewModel(savedState)
        viewModel.fillForm()
        payouts.createOutcome = Outcome.Failure(DomainError.ServiceUnavailable)

        // When
        viewModel.confirm()

        // Then
        assertEquals("400", savedState[PayoutViewModel.KEY_AMOUNT])
        assertEquals(VALID_IBAN, savedState[PayoutViewModel.KEY_IBAN])
    }

    @Test
    fun `should say the status is stale when following the payout gives up before it is final`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))

        // When
        viewModel.confirm()
        advanceUntilIdle()

        // Then
        assertEquals(
            PayoutPhase.Succeeded(payout(status = PayoutStatus.PROCESSING), isStatusStale = true),
            viewModel.phase,
        )
    }

    @Test
    fun `should say the status is stale when every status request fails`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Failure(DomainError.NoConnection)

        // When
        viewModel.confirm()
        advanceUntilIdle()

        // Then
        assertEquals(
            PayoutPhase.Succeeded(payout(status = PayoutStatus.PENDING), isStatusStale = true),
            viewModel.phase,
        )
    }

    @Test
    fun `should follow the payout again when the merchant asks to check the status again`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))
        viewModel.confirm()
        advanceUntilIdle()
        payouts.getOutcomes.clear()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))

        // When
        viewModel.onCheckPayoutStatusAgain()
        advanceUntilIdle()

        // Then
        assertEquals(PayoutPhase.Succeeded(payout(status = PayoutStatus.COMPLETED)), viewModel.phase)
    }

    @Test
    fun `should ignore a request to check the status again when updates are still coming in`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))
        viewModel.confirm()
        val requestsSoFar = payouts.getRequests

        // When
        viewModel.onCheckPayoutStatusAgain()

        // Then
        assertEquals(requestsSoFar, payouts.getRequests)
    }

    @Test
    fun `should report a failure when the payout ends up failed after being created`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.FAILED))

        // When
        viewModel.confirm()
        advanceUntilIdle()

        // Then
        assertEquals(PayoutPhase.Failed(PayoutFailure.ProcessingFailed), viewModel.phase)
    }

    @Test
    fun `should reset the form when the merchant creates another payout`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.COMPLETED))
        viewModel.confirm()
        advanceUntilIdle()

        // When
        viewModel.onCreateAnotherPayout()

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
        assertEquals(PayoutDraft("", Currency.GBP, ""), viewModel.uiState.value.draft)
    }

    @Test
    fun `should stop following the payout when the merchant creates another one`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.fillForm()
        payouts.getOutcomes += Outcome.Success(payout(status = PayoutStatus.PROCESSING))
        viewModel.confirm()

        // When
        viewModel.onCreateAnotherPayout()
        advanceUntilIdle()

        // Then
        assertEquals(PayoutPhase.Editing, viewModel.phase)
        assertEquals(0, payouts.getRequests)
    }
}
