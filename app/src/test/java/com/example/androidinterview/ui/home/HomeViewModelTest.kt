package com.example.androidinterview.ui.home

import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.usecase.GetMerchantOverviewUseCase
import com.example.androidinterview.testing.FakeMerchantRepository
import com.example.androidinterview.testing.activityItem
import com.example.androidinterview.testing.gbp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val merchants = FakeMerchantRepository()
    private val overview = MerchantOverview(
        availableBalance = gbp(500_000),
        pendingBalance = gbp(25_000),
        recentActivity = listOf(activityItem(id = "act_001")),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(GetMerchantOverviewUseCase(merchants))

    @Test
    fun `should show loading when the overview is still being fetched`() {
        // Given
        merchants.overviewGate = CompletableDeferred()
        merchants.enqueueOverview(Outcome.Success(overview))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `should show the overview when loading succeeds`() {
        // Given
        merchants.enqueueOverview(Outcome.Success(overview))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(HomeUiState.Success(overview), viewModel.uiState.value)
    }

    @Test
    fun `should show the error when loading fails`() {
        // Given
        merchants.enqueueOverview(Outcome.Failure(DomainError.NoConnection))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(HomeUiState.Error(DomainError.NoConnection), viewModel.uiState.value)
    }

    @Test
    fun `should load again when retry is requested after a failure`() {
        // Given
        merchants.enqueueOverview(Outcome.Failure(DomainError.ServiceUnavailable), Outcome.Success(overview))
        val viewModel = viewModel()

        // When
        viewModel.onRetry()

        // Then
        assertEquals(HomeUiState.Success(overview), viewModel.uiState.value)
        assertEquals(2, merchants.overviewRequests)
    }
}
