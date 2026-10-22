package com.example.androidinterview.ui.preview

import com.example.androidinterview.domain.model.ActivityDay
import com.example.androidinterview.domain.model.ActivityGroup
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityStatus
import com.example.androidinterview.domain.model.ActivityType
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutStatus
import com.example.androidinterview.domain.usecase.ValidatePayoutDraftUseCase
import com.example.androidinterview.domain.validation.PayoutDraft
import com.example.androidinterview.ui.payout.PayoutPhase
import com.example.androidinterview.ui.payout.PayoutUiState
import java.time.Instant
import java.time.LocalDate

/** Sample data for the `@Preview` functions, so every screen state can be seen without running the app. */
internal object PreviewData {

    const val IBAN = "GB29NWBK60161331926819"

    private fun item(
        id: String,
        type: ActivityType,
        minorUnits: Long,
        description: String,
        status: ActivityStatus = ActivityStatus.COMPLETED,
    ) = ActivityItem(
        id = id,
        type = type,
        amount = Money(minorUnits, Currency.GBP),
        occurredAt = Instant.parse("2026-10-20T10:00:00Z"),
        description = description,
        status = status,
    )

    private val items = listOf(
        item("act_001", ActivityType.PAYOUT, -12_500, "Invoice payment", ActivityStatus.PROCESSING),
        item("act_002", ActivityType.DEPOSIT, 75_000, "Payment received", ActivityStatus.PROCESSING),
        item("act_003", ActivityType.FEE, -1_500, "Monthly service fee"),
        item("act_004", ActivityType.REFUND, 10_000, "Refund from merchant"),
    )

    val overview = MerchantOverview(
        availableBalance = Money(500_000, Currency.GBP),
        pendingBalance = Money(25_000, Currency.GBP),
        recentActivity = items.take(3),
    )

    val activityGroups = listOf(
        ActivityGroup(ActivityDay.Today, items.take(2)),
        ActivityGroup(ActivityDay.Yesterday, items.drop(2)),
        ActivityGroup(ActivityDay.On(LocalDate.of(2025, 5, 18)), listOf(items.first().copy(id = "act_100"))),
    )

    val payout = Payout(
        id = "pay_001",
        status = PayoutStatus.PENDING,
        amount = Money(40_000, Currency.GBP),
        iban = IBAN,
        createdAt = Instant.parse("2026-10-20T10:00:00Z"),
    )

    fun payoutState(
        amountInput: String = "400",
        ibanInput: String = IBAN,
        phase: PayoutPhase = PayoutPhase.Editing,
    ): PayoutUiState {
        val draft = PayoutDraft(amountInput, Currency.GBP, ibanInput)
        return PayoutUiState(draft = draft, validation = ValidatePayoutDraftUseCase()(draft), phase = phase)
    }
}
