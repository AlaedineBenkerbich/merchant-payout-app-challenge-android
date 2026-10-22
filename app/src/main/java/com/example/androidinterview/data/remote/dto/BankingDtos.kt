package com.example.androidinterview.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of the banking API; amounts are in the lowest denomination (pence, cents).
 *
 * The enums that describe *what happened* (type, status) have an `UNKNOWN` member and are
 * decoded with `coerceInputValues`: a value the API adds later degrades to `UNKNOWN` instead of
 * making the whole page fail. A currency is different, since an amount cannot be shown or
 * trusted without it, so an unsupported currency still fails the response.
 */

@Serializable
enum class CurrencyDto { GBP, EUR }

@Serializable
enum class ActivityTypeDto {
    @SerialName("payout") PAYOUT,
    @SerialName("deposit") DEPOSIT,
    @SerialName("refund") REFUND,
    @SerialName("fee") FEE,
    UNKNOWN,
}

@Serializable
enum class ActivityStatusDto {
    @SerialName("completed") COMPLETED,
    @SerialName("pending") PENDING,
    @SerialName("processing") PROCESSING,
    @SerialName("failed") FAILED,
    UNKNOWN,
}

@Serializable
enum class PayoutStatusDto {
    @SerialName("pending") PENDING,
    @SerialName("processing") PROCESSING,
    @SerialName("completed") COMPLETED,
    @SerialName("failed") FAILED,
    UNKNOWN,
}

@Serializable
data class ActivityItemDto(
    val id: String,
    val type: ActivityTypeDto = ActivityTypeDto.UNKNOWN,
    val amount: Long,
    val currency: CurrencyDto,
    val date: String,
    val description: String,
    val status: ActivityStatusDto = ActivityStatusDto.UNKNOWN,
)

@Serializable
data class MerchantDataResponseDto(
    @SerialName("available_balance") val availableBalance: Long,
    @SerialName("pending_balance") val pendingBalance: Long,
    val currency: CurrencyDto,
    val activity: List<ActivityItemDto>,
)

@Serializable
data class PaginatedActivityResponseDto(
    val items: List<ActivityItemDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class CreatePayoutRequestDto(
    val amount: Long,
    val currency: CurrencyDto,
    val iban: String,
    @SerialName("device_id") val deviceId: String? = null,
)

@Serializable
data class PayoutResponseDto(
    val id: String,
    val status: PayoutStatusDto = PayoutStatusDto.UNKNOWN,
    val amount: Long,
    val currency: CurrencyDto,
    val iban: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class DeviceResponseDto(
    @SerialName("device_id") val deviceId: String,
)

/** Body of the 4xx / 5xx answers, e.g. `{"error":"Insufficient funds","code":"INSUFFICIENT_FUNDS"}`. */
@Serializable
data class ApiErrorDto(
    val error: String? = null,
    val code: String? = null,
)
