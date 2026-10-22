package com.example.androidinterview.testing

import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityStatus
import com.example.androidinterview.domain.model.ActivityType
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.Iban
import com.example.androidinterview.domain.model.IbanValidation
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.domain.validation.ValidatedPayout
import java.time.Instant

const val VALID_IBAN = "GB29NWBK60161331926819"

fun gbp(minorUnits: Long) = Money(minorUnits, Currency.GBP)

fun eur(minorUnits: Long) = Money(minorUnits, Currency.EUR)

fun validIban(): Iban = (Iban.parse(VALID_IBAN) as IbanValidation.Valid).iban

fun validatedPayout(amount: Money = gbp(40_000)) = ValidatedPayout(amount = amount, iban = validIban())

fun activityItem(
    id: String = "act_001",
    amount: Long = 1_000,
    occurredAt: Instant = Instant.parse("2026-01-01T10:00:00Z"),
    type: ActivityType = ActivityType.DEPOSIT,
    status: ActivityStatus = ActivityStatus.COMPLETED,
    description: String = "Payment received",
) = ActivityItem(
    id = id,
    type = type,
    amount = gbp(amount),
    occurredAt = occurredAt,
    description = description,
    status = status,
)

fun payout(
    id: String = "pay_001",
    status: PayoutStatus = PayoutStatus.PENDING,
    amount: Money = gbp(40_000),
) = Payout(
    id = id,
    status = status,
    amount = amount,
    iban = VALID_IBAN,
    createdAt = Instant.parse("2026-01-01T10:00:00Z"),
)
