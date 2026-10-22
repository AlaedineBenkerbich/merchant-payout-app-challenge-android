package com.example.androidinterview.ui.payout

/**
 * Keeps what an amount can be written with (digits, a leading minus, `.` and `,`) and drops
 * everything else, such as a pasted currency symbol, spaces or letters.
 *
 * Nothing is reinterpreted here: how the separators are read is decided by the validator, which
 * can tell the merchant when an amount is ambiguous instead of quietly paying a different one.
 */
internal fun sanitizeAmountInput(raw: String): String = buildString {
    for (character in raw) {
        when {
            character in '0'..'9' || character == '.' || character == ',' -> append(character)
            character == '-' && isEmpty() -> append(character)
        }
    }
}
