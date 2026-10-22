package com.example.androidinterview.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        state: HomeUiState,
        onRetry: () -> Unit = {},
        onShowMore: () -> Unit = {},
    ) {
        compose.setContent {
            AndroidInterviewTheme {
                HomeScreen(state = state, onRetry = onRetry, onShowMore = onShowMore)
            }
        }
    }

    @Test
    fun `should show both balances and the recent activity when the overview is loaded`() {
        // Given / When
        setContent(HomeUiState.Success(PreviewData.overview))

        // Then
        compose.onNodeWithText("£5,000.00").assertIsDisplayed()
        compose.onNodeWithText("£250.00").assertIsDisplayed()
        compose.onNodeWithText("Invoice payment").assertIsDisplayed()
        compose.onNodeWithText("-£125.00").assertIsDisplayed()
        compose.onNodeWithText("Payment received").assertIsDisplayed()
        compose.onNodeWithText("£750.00").assertIsDisplayed()
    }

    @Test
    fun `should open the full activity list when show more is pressed`() {
        // Given
        var showMoreCount = 0
        setContent(HomeUiState.Success(PreviewData.overview), onShowMore = { showMoreCount++ })

        // When
        compose.onNodeWithText("Show More").performClick()

        // Then
        assertEquals(1, showMoreCount)
    }

    @Test
    fun `should show a loading indicator while the overview is loading`() {
        // Given / When
        setContent(HomeUiState.Loading)

        // Then
        compose.onNodeWithContentDescription("Loading your account").assertIsDisplayed()
    }

    @Test
    fun `should explain the failure and offer to retry when the overview could not be loaded`() {
        // Given
        var retryCount = 0
        setContent(HomeUiState.Error(DomainError.NoConnection), onRetry = { retryCount++ })

        // When
        compose.onNodeWithText("No internet connection. Check your connection and try again.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        // Then
        assertEquals(1, retryCount)
    }
}
