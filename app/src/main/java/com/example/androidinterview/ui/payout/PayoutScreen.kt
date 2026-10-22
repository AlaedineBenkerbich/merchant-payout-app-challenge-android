package com.example.androidinterview.ui.payout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidinterview.R
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.IbanError
import com.example.androidinterview.domain.model.IbanValidation
import com.example.androidinterview.domain.validation.AmountValidation
import com.example.androidinterview.ui.common.ScreenTitle
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme

@Composable
fun PayoutRoute(
    viewModel: PayoutViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showScreenshotWarning by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // These effects are tied to this screen: they stop as soon as the merchant leaves the payout tab.
    SecureWindowEffect()
    ScreenshotDetectionEffect(onScreenshotTaken = { showScreenshotWarning = true })
    BiometricPromptEffect(phase = state.phase, onResult = viewModel::onBiometricResult)

    // Leaving while the request is on its way would destroy the view model and lose the outcome of
    // a payout that the server may well have created. The overlay already blocks touches; this blocks Back.
    BackHandler(enabled = state.phase is PayoutPhase.Submitting) {}

    PayoutScreen(
        state = state,
        onAmountChanged = viewModel::onAmountChanged,
        onCurrencySelected = viewModel::onCurrencySelected,
        onIbanChanged = viewModel::onIbanChanged,
        onConfirmClicked = viewModel::onConfirmClicked,
        onConfirmationDismissed = viewModel::onConfirmationDismissed,
        onConfirmationAccepted = viewModel::onConfirmationAccepted,
        onTryAgain = viewModel::onTryAgain,
        onCreateAnotherPayout = viewModel::onCreateAnotherPayout,
        onCheckPayoutStatusAgain = viewModel::onCheckPayoutStatusAgain,
        onOpenBiometricSettings = { context.openBiometricSettings() },
        modifier = modifier,
    )

    if (showScreenshotWarning) {
        ScreenshotWarningDialog(onDismiss = { showScreenshotWarning = false })
    }
}

@Composable
fun PayoutScreen(
    state: PayoutUiState,
    onAmountChanged: (String) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onIbanChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    onConfirmationAccepted: () -> Unit,
    onTryAgain: () -> Unit,
    onCreateAnotherPayout: () -> Unit,
    onCheckPayoutStatusAgain: () -> Unit,
    onOpenBiometricSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        when (val phase = state.phase) {
            is PayoutPhase.Succeeded -> {
                ScreenTitle(text = stringResource(R.string.payout_result_title))
                PayoutSucceededContent(
                    payout = phase.payout,
                    isStatusStale = phase.isStatusStale,
                    onCheckStatusAgain = onCheckPayoutStatusAgain,
                    onCreateAnotherPayout = onCreateAnotherPayout,
                )
            }

            is PayoutPhase.Failed -> {
                ScreenTitle(text = stringResource(R.string.payout_result_title))
                PayoutFailedContent(
                    failure = phase.failure,
                    onTryAgain = onTryAgain,
                    onOpenBiometricSettings = onOpenBiometricSettings,
                )
            }

            else -> {
                ScreenTitle(text = stringResource(R.string.payout_title))
                PayoutForm(
                    state = state,
                    onAmountChanged = onAmountChanged,
                    onCurrencySelected = onCurrencySelected,
                    onIbanChanged = onIbanChanged,
                    onConfirmClicked = onConfirmClicked,
                    onConfirmationDismissed = onConfirmationDismissed,
                    onConfirmationAccepted = onConfirmationAccepted,
                )
            }
        }
    }
}

@Composable
private fun PayoutForm(
    state: PayoutUiState,
    onAmountChanged: (String) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onIbanChanged: (String) -> Unit,
    onConfirmClicked: () -> Unit,
    onConfirmationDismissed: () -> Unit,
    onConfirmationAccepted: () -> Unit,
) {
    val phase = state.phase
    // Inputs are frozen from the moment the merchant confirms until the flow ends.
    val fieldsEnabled = phase is PayoutPhase.Editing

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    value = state.draft.amountInput,
                    validation = state.validation.amount,
                    currency = state.draft.currency,
                    enabled = fieldsEnabled,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.weight(2f),
                )
                CurrencyDropdown(
                    selected = state.draft.currency,
                    enabled = fieldsEnabled,
                    onSelected = onCurrencySelected,
                    modifier = Modifier.weight(1f),
                )
            }
            IbanField(
                value = state.draft.ibanInput,
                validation = state.validation.iban,
                enabled = fieldsEnabled,
                onValueChange = onIbanChanged,
            )
            Spacer(Modifier.height(8.dp))
            // A disabled button gives a screen-reader user no clue why; say what is missing.
            val disabledHint = stringResource(R.string.payout_confirm_disabled_hint)
            Button(
                onClick = onConfirmClicked,
                enabled = state.canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .semantics { if (!state.canConfirm) stateDescription = disabledHint },
            ) {
                Text(text = stringResource(R.string.payout_confirm))
            }
        }

        if (phase is PayoutPhase.Submitting) SubmittingOverlay()
    }

    if (phase is PayoutPhase.Confirming) {
        ConfirmPayoutDialog(
            payout = phase.payout,
            onConfirm = onConfirmationAccepted,
            onDismiss = onConfirmationDismissed,
        )
    }
}

@Composable
private fun AmountField(
    value: String,
    validation: AmountValidation,
    currency: Currency,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMessage = validation.errorMessage(currency)
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(text = stringResource(R.string.payout_amount_label)) },
        placeholder = { Text(text = stringResource(R.string.payout_amount_placeholder)) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(text = message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        // "Next" goes to the IBAN below, not to the currency picker that sits beside the amount.
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: Currency,
    enabled: Boolean,
    onSelected: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(text = stringResource(R.string.payout_currency_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(text = currency.name) },
                    onClick = {
                        onSelected(currency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun IbanField(
    value: String,
    validation: IbanValidation,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // An incomplete IBAN is only worth flagging once the merchant has finished with the field:
    // complaining about every half-typed one would be noise. Something that is already wrong
    // (a forbidden character, too many characters, a failed checksum) is flagged straight away.
    var hasHadFocus by rememberSaveable { mutableStateOf(false) }
    var hasLeftField by rememberSaveable { mutableStateOf(false) }
    val showError = validation is IbanValidation.Invalid &&
        (hasLeftField || validation.reason != IbanError.TOO_SHORT)
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(text = stringResource(R.string.payout_iban_label)) },
        placeholder = { Text(text = stringResource(R.string.payout_iban_placeholder)) },
        isError = showError,
        supportingText = {
            Text(text = stringResource(if (showError) R.string.payout_iban_error else R.string.payout_iban_helper))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        // Leaving the field with "Done" also dismisses the keyboard and reveals any IBAN error.
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focus ->
                if (focus.isFocused) {
                    hasHadFocus = true
                } else if (hasHadFocus) {
                    hasLeftField = true
                }
            },
    )
}

/** Blocks the form while the payout is being sent, so it cannot be edited or sent twice. */
@Composable
private fun SubmittingOverlay() {
    val description = stringResource(R.string.payout_processing)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            // Swallow touches so nothing underneath can be pressed.
            .pointerInput(Unit) {}
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AmountValidation.errorMessage(currency: Currency): String? = when (this) {
    AmountValidation.NotANumber -> stringResource(R.string.payout_amount_error_invalid)
    AmountValidation.NotPositive -> stringResource(R.string.payout_amount_error_not_positive)
    AmountValidation.TooManyDecimals -> pluralStringResource(
        R.plurals.payout_amount_error_too_many_decimals,
        currency.fractionDigits,
        currency.fractionDigits,
    )
    AmountValidation.TooLarge -> stringResource(R.string.payout_amount_error_too_large)
    // Nothing to flag while the field is empty or holds only a sign or a separator: the button just stays off.
    AmountValidation.Empty,
    is AmountValidation.Valid,
    -> null
}

@Composable
private fun PayoutPreview(state: PayoutUiState) {
    AndroidInterviewTheme {
        PayoutScreen(
            state = state,
            onAmountChanged = {},
            onCurrencySelected = {},
            onIbanChanged = {},
            onConfirmClicked = {},
            onConfirmationDismissed = {},
            onConfirmationAccepted = {},
            onTryAgain = {},
            onCreateAnotherPayout = {},
            onCheckPayoutStatusAgain = {},
            onOpenBiometricSettings = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty form")
@Composable
private fun PayoutEmptyPreview() {
    PayoutPreview(PreviewData.payoutState(amountInput = "", ibanInput = ""))
}

@Preview(showBackground = true, name = "Valid form")
@Composable
private fun PayoutValidPreview() {
    PayoutPreview(PreviewData.payoutState())
}

@Preview(showBackground = true, name = "Confirmation")
@Composable
private fun PayoutConfirmingPreview() {
    val state = PreviewData.payoutState()
    PayoutPreview(state.copy(phase = PayoutPhase.Confirming(state.validation.validatedPayout!!)))
}

@Preview(showBackground = true, name = "Submitted")
@Composable
private fun PayoutSubmittedPreview() {
    PayoutPreview(PreviewData.payoutState(phase = PayoutPhase.Succeeded(PreviewData.payout)))
}

@Preview(showBackground = true, name = "Status not updating")
@Composable
private fun PayoutStaleStatusPreview() {
    PayoutPreview(
        PreviewData.payoutState(phase = PayoutPhase.Succeeded(PreviewData.payout, isStatusStale = true)),
    )
}

@Preview(showBackground = true, name = "Failed")
@Composable
private fun PayoutFailedPreview() {
    PayoutPreview(PreviewData.payoutState(phase = PayoutPhase.Failed(PayoutFailure.InsufficientFunds)))
}
