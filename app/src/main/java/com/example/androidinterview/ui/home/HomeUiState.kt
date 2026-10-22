package com.example.androidinterview.ui.home

import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.MerchantOverview

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val error: DomainError) : HomeUiState
    data class Success(val overview: MerchantOverview) : HomeUiState
}
