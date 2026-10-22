package com.example.androidinterview.data.mapper

import com.example.androidinterview.data.remote.dto.ActivityItemDto
import com.example.androidinterview.data.remote.dto.ActivityStatusDto
import com.example.androidinterview.data.remote.dto.ActivityTypeDto
import com.example.androidinterview.data.remote.dto.CreatePayoutRequestDto
import com.example.androidinterview.data.remote.dto.CurrencyDto
import com.example.androidinterview.data.remote.dto.MerchantDataResponseDto
import com.example.androidinterview.data.remote.dto.PaginatedActivityResponseDto
import com.example.androidinterview.data.remote.dto.PayoutResponseDto
import com.example.androidinterview.data.remote.dto.PayoutStatusDto
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.ActivityStatus
import com.example.androidinterview.domain.model.ActivityType
import com.example.androidinterview.domain.model.Currency
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.domain.model.Payout
import com.example.androidinterview.domain.model.PayoutRequest
import com.example.androidinterview.domain.model.PayoutStatus
import java.time.OffsetDateTime

internal fun CurrencyDto.toDomain(): Currency = when (this) {
    CurrencyDto.GBP -> Currency.GBP
    CurrencyDto.EUR -> Currency.EUR
}

internal fun Currency.toDto(): CurrencyDto = when (this) {
    Currency.GBP -> CurrencyDto.GBP
    Currency.EUR -> CurrencyDto.EUR
}

private fun ActivityTypeDto.toDomain(): ActivityType = when (this) {
    ActivityTypeDto.PAYOUT -> ActivityType.PAYOUT
    ActivityTypeDto.DEPOSIT -> ActivityType.DEPOSIT
    ActivityTypeDto.REFUND -> ActivityType.REFUND
    ActivityTypeDto.FEE -> ActivityType.FEE
    ActivityTypeDto.UNKNOWN -> ActivityType.OTHER
}

private fun ActivityStatusDto.toDomain(): ActivityStatus = when (this) {
    ActivityStatusDto.COMPLETED -> ActivityStatus.COMPLETED
    ActivityStatusDto.PENDING -> ActivityStatus.PENDING
    ActivityStatusDto.PROCESSING -> ActivityStatus.PROCESSING
    ActivityStatusDto.FAILED -> ActivityStatus.FAILED
    ActivityStatusDto.UNKNOWN -> ActivityStatus.UNKNOWN
}

private fun PayoutStatusDto.toDomain(): PayoutStatus = when (this) {
    PayoutStatusDto.PENDING -> PayoutStatus.PENDING
    PayoutStatusDto.PROCESSING -> PayoutStatus.PROCESSING
    PayoutStatusDto.COMPLETED -> PayoutStatus.COMPLETED
    PayoutStatusDto.FAILED -> PayoutStatus.FAILED
    PayoutStatusDto.UNKNOWN -> PayoutStatus.UNKNOWN
}

internal fun ActivityItemDto.toDomain(): ActivityItem = ActivityItem(
    id = id,
    type = type.toDomain(),
    amount = Money(amount, currency.toDomain()),
    occurredAt = OffsetDateTime.parse(date).toInstant(),
    description = description,
    status = status.toDomain(),
)

internal fun MerchantDataResponseDto.toDomain(): MerchantOverview {
    val accountCurrency = currency.toDomain()
    return MerchantOverview(
        availableBalance = Money(availableBalance, accountCurrency),
        pendingBalance = Money(pendingBalance, accountCurrency),
        recentActivity = activity.map(ActivityItemDto::toDomain),
    )
}

internal fun PaginatedActivityResponseDto.toDomain(): ActivityPage = ActivityPage(
    items = items.map(ActivityItemDto::toDomain),
    nextCursor = nextCursor,
    hasMore = hasMore,
)

internal fun PayoutResponseDto.toDomain(): Payout = Payout(
    id = id,
    status = status.toDomain(),
    amount = Money(amount, currency.toDomain()),
    iban = iban,
    createdAt = OffsetDateTime.parse(createdAt).toInstant(),
)

internal fun PayoutRequest.toDto(): CreatePayoutRequestDto = CreatePayoutRequestDto(
    amount = amount.minorUnits,
    currency = amount.currency.toDto(),
    iban = iban.value,
    deviceId = deviceId,
)
