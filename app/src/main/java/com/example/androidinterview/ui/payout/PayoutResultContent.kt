package com.example.androidinterview.ui.payout

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidinterview.R
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.ui.common.MoneyFormatter
import com.example.androidinterview.ui.theme.appColors

/**
 * Confirmation of a created payout. It says "completed" only once the API does: until then the
 * merchant sees that the payout was submitted and is still being processed.
 */
@Composable
internal fun PayoutSucceededContent(
    payout: Payout,
    isStatusStale: Boolean,
    onCheckStatusAgain: () -> Unit,
    onCreateAnotherPayout: () -> Unit,
) {
    val amount = MoneyFormatter.format(payout.amount)
    val completed = payout.status == PayoutStatus.COMPLETED

    PayoutResultLayout(
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.appColors.credit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        },
        title = stringResource(if (completed) R.string.payout_completed_title else R.string.payout_submitted_title),
        titleColor = MaterialTheme.colorScheme.onBackground,
        message = if (completed) {
            stringResource(R.string.payout_completed_message, amount)
        } else {
            stringResource(R.string.payout_submitted_message, amount)
        },
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
        primaryLabel = stringResource(R.string.payout_create_another),
        onPrimary = onCreateAnotherPayout,
        extra = {
            if (!payout.status.isFinal) {
                Spacer(Modifier.height(24.dp))
                if (isStatusStale) {
                    // Updates stopped: no spinner that would promise a result which is not being awaited.
                    Text(
                        text = stringResource(R.string.payout_status_stale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onCheckStatusAgain) {
                        Text(text = stringResource(R.string.payout_check_status_again))
                    }
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(payout.status.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
internal fun PayoutFailedContent(
    failure: PayoutFailure,
    onTryAgain: () -> Unit,
    onOpenBiometricSettings: () -> Unit,
) {
    val errorColor = MaterialTheme.colorScheme.error

    PayoutResultLayout(
        icon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = errorColor,
                modifier = Modifier.size(64.dp),
            )
        },
        title = stringResource(R.string.payout_failed_title),
        titleColor = errorColor,
        message = failure.message(),
        messageColor = errorColor,
        primaryLabel = stringResource(R.string.action_try_again),
        onPrimary = onTryAgain,
        secondaryLabel = if (failure is PayoutFailure.BiometricNotSetUp) {
            stringResource(R.string.biometric_open_settings)
        } else {
            null
        },
        onSecondary = onOpenBiometricSettings,
    )
}

@Composable
private fun PayoutResultLayout(
    icon: @Composable () -> Unit,
    title: String,
    titleColor: Color,
    message: String,
    messageColor: Color,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    extra: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = messageColor,
                textAlign = TextAlign.Center,
            )
            extra()
            Spacer(Modifier.height(32.dp))
            Button(onClick = onPrimary, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(text = primaryLabel)
            }
            if (secondaryLabel != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSecondary) {
                    Text(text = secondaryLabel)
                }
            }
        }
    }
}

@Composable
private fun PayoutFailure.message(): String = when (this) {
    PayoutFailure.InsufficientFunds -> stringResource(R.string.error_insufficient_funds)
    PayoutFailure.ServiceUnavailable -> stringResource(R.string.error_service_unavailable)
    PayoutFailure.NoConnection -> stringResource(R.string.error_no_connection)
    is PayoutFailure.Rejected -> stringResource(R.string.error_rejected)
    PayoutFailure.Unexpected -> stringResource(R.string.error_unexpected)
    PayoutFailure.ProcessingFailed -> stringResource(R.string.payout_failure_processing)
    PayoutFailure.BiometricCancelled -> stringResource(R.string.biometric_failure_cancelled)
    is PayoutFailure.BiometricNotSetUp ->
        stringResource(R.string.biometric_failure_not_set_up, MoneyFormatter.format(threshold))
    PayoutFailure.BiometricLockedOut -> stringResource(R.string.biometric_failure_locked_out)
    PayoutFailure.BiometricUnavailable -> stringResource(R.string.biometric_failure_unavailable)
}

@StringRes
private fun PayoutStatus.labelRes(): Int = when (this) {
    PayoutStatus.PENDING -> R.string.status_pending
    PayoutStatus.PROCESSING -> R.string.status_processing
    PayoutStatus.COMPLETED -> R.string.status_completed
    PayoutStatus.FAILED -> R.string.status_failed
    PayoutStatus.UNKNOWN -> R.string.status_unknown
}
