package com.example.androidinterview.domain.validation

import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Iban
import com.example.androidinterview.domain.model.IbanValidation
import com.example.androidinterview.domain.model.Money

/** What the merchant has typed so far in the payout form. */
data class PayoutDraft(
    val amountInput: String,
    val currency: Currency,
    val ibanInput: String,
)

/** A payout whose amount and IBAN both passed validation. */
data class ValidatedPayout(
    val amount: Money,
    val iban: Iban,
)

data class PayoutDraftValidation(
    val amount: AmountValidation,
    val iban: IbanValidation,
) {
    /** Non-null only when the whole draft can be submitted. */
    val validatedPayout: ValidatedPayout?
        get() {
            val validAmount = amount as? AmountValidation.Valid ?: return null
            val validIban = iban as? IbanValidation.Valid ?: return null
            return ValidatedPayout(validAmount.money, validIban.iban)
        }
}
