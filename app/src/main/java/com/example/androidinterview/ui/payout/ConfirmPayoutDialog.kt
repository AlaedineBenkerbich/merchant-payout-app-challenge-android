package com.example.androidinterview.ui.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidinterview.R
import com.example.androidinterview.domain.validation.ValidatedPayout
import com.example.androidinterview.ui.common.MoneyFormatter

/** Summary shown before anything is sent: the IBAN is masked so it is not exposed on screen. */
@Composable
internal fun ConfirmPayoutDialog(
    payout: ValidatedPayout,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ibanDescription = stringResource(
        R.string.payout_confirm_iban_description,
        payout.iban.value.take(IBAN_SPOKEN_EDGE),
        payout.iban.value.takeLast(IBAN_SPOKEN_EDGE),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.payout_confirm_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column {
                SummaryRow(
                    label = stringResource(R.string.payout_confirm_amount),
                    value = MoneyFormatter.format(payout.amount),
                )
                HorizontalDivider()
                SummaryRow(
                    label = stringResource(R.string.payout_confirm_currency),
                    value = payout.amount.currency.name,
                )
                HorizontalDivider()
                SummaryRow(
                    label = stringResource(R.string.payout_confirm_iban),
                    value = payout.iban.masked,
                    // A screen reader would otherwise spell out every asterisk.
                    valueDescription = ibanDescription,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.payout_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

private const val IBAN_SPOKEN_EDGE = 4

@Composable
private fun SummaryRow(label: String, value: String, valueDescription: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .semantics { valueDescription?.let { contentDescription = it } },
        )
    }
}
