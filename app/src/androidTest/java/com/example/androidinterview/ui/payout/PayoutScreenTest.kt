package com.example.androidinterview.ui.payout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class PayoutScreenTest {

    @get:Rule
    val compose = createComposeRule()

    /** Counts what the screen asks its view model to do. */
    private class Calls {
        var confirmClicked = 0
        var confirmationAccepted = 0
        var confirmationDismissed = 0
        var tryAgain = 0
        var createAnother = 0
        var checkStatusAgain = 0
        var openBiometricSettings = 0
        var typedAmount: String? = null
    }

    private val calls = Calls()

    private fun setContent(state: PayoutUiState) {
        compose.setContent {
            AndroidInterviewTheme {
                PayoutScreen(
                    state = state,
                    onAmountChanged = { calls.typedAmount = it },
                    onCurrencySelected = {},
                    onIbanChanged = {},
                    onConfirmClicked = { calls.confirmClicked++ },
                    onConfirmationDismissed = { calls.confirmationDismissed++ },
                    onConfirmationAccepted = { calls.confirmationAccepted++ },
                    onTryAgain = { calls.tryAgain++ },
                    onCreateAnotherPayout = { calls.createAnother++ },
                    onCheckPayoutStatusAgain = { calls.checkStatusAgain++ },
                    onOpenBiometricSettings = { calls.openBiometricSettings++ },
                )
            }
        }
    }

    private fun confirmingState(): PayoutUiState {
        val state = PreviewData.payoutState()
        return state.copy(phase = PayoutPhase.Confirming(state.validation.validatedPayout!!))
    }

    // --- Form ---------------------------------------------------------------------------------

    @Test
    fun `should keep confirm disabled when the form is empty`() {
        // Given / When
        setContent(PreviewData.payoutState(amountInput = "", ibanInput = ""))

        // Then
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun `should enable confirm and ask for confirmation when amount and iban are valid`() {
        // Given
        setContent(PreviewData.payoutState())

        // When
        compose.onNodeWithText("Confirm").assertIsEnabled().performClick()

        // Then
        assertEquals(1, calls.confirmClicked)
    }

    @Test
    fun `should report what the merchant types when the amount changes`() {
        // Given
        setContent(PreviewData.payoutState(amountInput = "", ibanInput = ""))

        // When
        compose.onNodeWithText("Amount").performTextInput("12")

        // Then
        assertEquals("12", calls.typedAmount)
    }

    @Test
    fun `should explain why when the amount is not above zero`() {
        // Given / When
        setContent(PreviewData.payoutState(amountInput = "0"))

        // Then
        compose.onNodeWithText("Amount must be greater than zero.").assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun `should explain why when the amount is ambiguous`() {
        // Given / When
        setContent(PreviewData.payoutState(amountInput = "1,2500"))

        // Then
        compose.onNodeWithText("Enter a valid amount, for example 1,250.50.").assertIsDisplayed()
    }

    @Test
    fun `should flag an iban that is already wrong without waiting for the field to be left`() {
        // Given / When: a full-length IBAN whose checksum is wrong.
        setContent(PreviewData.payoutState(ibanInput = "GB29NWBK60161331926818"))

        // Then
        compose.onNodeWithText("Enter a valid IBAN.").assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }

    @Test
    fun `should stay quiet about an incomplete iban while the merchant has not left the field`() {
        // Given / When
        setContent(PreviewData.payoutState(ibanInput = "GB29NWBK"))

        // Then
        compose.onNodeWithText("Enter the destination bank account IBAN").assertIsDisplayed()
        compose.onNodeWithText("Enter a valid IBAN.").assertDoesNotExist()
    }

    // --- Confirmation -------------------------------------------------------------------------

    @Test
    fun `should summarise the payout with a masked iban before anything is sent`() {
        // Given / When
        setContent(confirmingState())

        // Then: the form behind the dialog still holds what was typed, so the IBAN is looked for in the dialog only.
        val inDialog = hasAnyAncestor(isDialog())
        compose.onNodeWithText("Confirm Payout").assertIsDisplayed()
        compose.onNode(hasText("£400.00") and inDialog).assertIsDisplayed()
        compose.onNode(hasText("GB29**************6819") and inDialog).assertIsDisplayed()
        compose.onNode(hasText(PreviewData.IBAN) and inDialog).assertDoesNotExist()
    }

    @Test
    fun `should send the confirmation to the view model when confirm is pressed in the dialog`() {
        // Given
        setContent(confirmingState())

        // When
        compose.onNode(hasText("Confirm") and hasAnyAncestor(isDialog())).performClick()

        // Then
        assertEquals(1, calls.confirmationAccepted)
    }

    @Test
    fun `should go back to the form when cancel is pressed in the dialog`() {
        // Given
        setContent(confirmingState())

        // When
        compose.onNodeWithText("Cancel").performClick()

        // Then
        assertEquals(1, calls.confirmationDismissed)
    }

    @Test
    fun `should block the form while the payout is being sent`() {
        // Given / When
        val state = PreviewData.payoutState()
        setContent(state.copy(phase = PayoutPhase.Submitting(state.validation.validatedPayout!!)))

        // Then
        compose.onNodeWithContentDescription("Processing payout…").assertIsDisplayed()
    }

    // --- Results ------------------------------------------------------------------------------

    @Test
    fun `should say the payout was submitted and is still being processed when it is not final yet`() {
        // Given / When
        setContent(PreviewData.payoutState(phase = PayoutPhase.Succeeded(PreviewData.payout)))

        // Then
        compose.onNodeWithText("Payout Submitted").assertIsDisplayed()
        compose.onNodeWithText("Your payout of £400.00 is being processed.").assertIsDisplayed()
        compose.onNodeWithText("Pending").assertIsDisplayed()
    }

    @Test
    fun `should say the payout is completed only when the api says so`() {
        // Given
        val completed = PreviewData.payout.copy(status = PayoutStatus.COMPLETED)

        // When
        setContent(PreviewData.payoutState(phase = PayoutPhase.Succeeded(completed)))

        // Then
        compose.onNodeWithText("Payout Completed").assertIsDisplayed()
        compose.onNodeWithText("Your payout of £400.00 has been processed successfully.").assertIsDisplayed()
    }

    @Test
    fun `should offer to check again when following the payout has stopped before it is final`() {
        // Given
        setContent(PreviewData.payoutState(phase = PayoutPhase.Succeeded(PreviewData.payout, isStatusStale = true)))

        // When
        compose.onNodeWithText("This is taking longer than usual. Your payout is still being processed.")
            .assertIsDisplayed()
        compose.onNodeWithText("Check again").performClick()

        // Then
        assertEquals(1, calls.checkStatusAgain)
    }

    @Test
    fun `should start a new payout when create another payout is pressed`() {
        // Given
        setContent(PreviewData.payoutState(phase = PayoutPhase.Succeeded(PreviewData.payout)))

        // When
        compose.onNodeWithText("Create Another Payout").performClick()

        // Then
        assertEquals(1, calls.createAnother)
    }

    @Test
    fun `should show the insufficient funds message and let the merchant try again`() {
        // Given
        setContent(PreviewData.payoutState(phase = PayoutPhase.Failed(PayoutFailure.InsufficientFunds)))

        // When
        compose.onNodeWithText("Unable to Process Payout").assertIsDisplayed()
        compose.onNodeWithText("Insufficient funds.").assertIsDisplayed()
        compose.onNodeWithText("Try Again").performClick()

        // Then
        assertEquals(1, calls.tryAgain)
    }

    @Test
    fun `should tell the merchant to set up biometrics and offer the settings when none is enrolled`() {
        // Given
        val threshold = Money(100_000, Currency.GBP)
        setContent(PreviewData.payoutState(phase = PayoutPhase.Failed(PayoutFailure.BiometricNotSetUp(threshold))))

        // When
        compose.onNodeWithText(
            "Biometrics aren't set up on this device. Set up fingerprint authentication in Settings to send payouts over £1,000.00.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Open Settings").performClick()

        // Then
        assertEquals(1, calls.openBiometricSettings)
    }

    @Test
    fun `should say that biometric authentication was cancelled when the merchant dismissed the prompt`() {
        // Given / When
        setContent(PreviewData.payoutState(phase = PayoutPhase.Failed(PayoutFailure.BiometricCancelled)))

        // Then
        compose.onNodeWithText("Biometric authentication was cancelled.").assertIsDisplayed()
    }
}
