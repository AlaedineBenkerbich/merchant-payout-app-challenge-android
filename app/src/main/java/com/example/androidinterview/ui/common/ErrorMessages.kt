package com.example.androidinterview.ui.common

import androidx.annotation.StringRes
import com.example.androidinterview.R
import com.example.androidinterview.domain.model.DomainError

@StringRes
fun DomainError.toMessageRes(): Int = when (this) {
    DomainError.NoConnection -> R.string.error_no_connection
    DomainError.ServiceUnavailable -> R.string.error_service_unavailable
    DomainError.InsufficientFunds -> R.string.error_insufficient_funds
    is DomainError.Rejected -> R.string.error_rejected
    DomainError.Unexpected -> R.string.error_unexpected
}
