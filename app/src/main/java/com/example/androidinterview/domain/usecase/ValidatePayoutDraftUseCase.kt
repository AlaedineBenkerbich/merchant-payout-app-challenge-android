package com.example.androidinterview.domain.usecase

import com.example.androidinterview.domain.model.Iban
import com.example.androidinterview.domain.validation.PayoutAmountValidator
import com.example.androidinterview.domain.validation.PayoutDraft
import com.example.androidinterview.domain.validation.PayoutDraftValidation

class ValidatePayoutDraftUseCase {

    operator fun invoke(draft: PayoutDraft): PayoutDraftValidation = PayoutDraftValidation(
        amount = PayoutAmountValidator.validate(draft.amountInput, draft.currency),
        iban = Iban.parse(draft.ibanInput),
    )
}
