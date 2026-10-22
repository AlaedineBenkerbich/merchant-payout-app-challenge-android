package com.example.androidinterview.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.usecase.GetMerchantOverviewUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val getMerchantOverview: GetMerchantOverviewUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onRetry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            _uiState.value = when (val outcome = getMerchantOverview()) {
                is Outcome.Success -> HomeUiState.Success(outcome.value)
                is Outcome.Failure -> HomeUiState.Error(outcome.error)
            }
        }
    }
}
