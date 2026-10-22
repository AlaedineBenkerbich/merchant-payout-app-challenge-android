package com.example.androidinterview.ui.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.ui.common.ActivityDateFormatter
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class ActivityListScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        state: ActivityListUiState,
        onDone: () -> Unit = {},
        onRetry: () -> Unit = {},
        onRetryLoadMore: () -> Unit = {},
    ) {
        compose.setContent {
            AndroidInterviewTheme {
                ActivityListScreen(
                    state = state,
                    onDone = onDone,
                    onRetry = onRetry,
                    onLoadMore = {},
                    onRetryLoadMore = onRetryLoadMore,
                )
            }
        }
    }

    private fun content(footer: PagingFooter) = ActivityListUiState.Content(PreviewData.activityGroups, footer)

    @Test
    fun `should group the rows under today yesterday and a dated heading when the list is loaded`() {
        // Given / When
        setContent(content(PagingFooter.Idle))

        // Then
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("Yesterday").assertIsDisplayed()
        compose.onNodeWithText("18 May 2025").assertIsDisplayed()
    }

    @Test
    fun `should show type description date amount and status together on one row`() {
        // Given / When
        setContent(content(PagingFooter.Idle))

        // Then: a row is a single accessibility node, so its texts are matched together (the same status
        // appears on several rows, which is why they are not matched one by one).
        val date = ActivityDateFormatter.format(Instant.parse("2026-10-20T10:00:00Z"))
        compose.onNode(
            hasText("Deposit") and hasText("Payment received") and hasText(date) and
                hasText("£750.00") and hasText("Processing"),
        ).assertIsDisplayed()
    }

    @Test
    fun `should close the list when done is pressed`() {
        // Given
        var doneCount = 0
        setContent(content(PagingFooter.Idle), onDone = { doneCount++ })

        // When
        compose.onNodeWithText("Done").performClick()

        // Then
        assertEquals(1, doneCount)
    }

    @Test
    fun `should tell the merchant that more items are loading when the next page is on its way`() {
        // Given / When
        setContent(content(PagingFooter.Loading))

        // Then
        compose.onNodeWithText("Loading more…").assertIsDisplayed()
    }

    @Test
    fun `should offer to retry the next page when it failed to load`() {
        // Given
        var retryCount = 0
        setContent(content(PagingFooter.Error(DomainError.NoConnection)), onRetryLoadMore = { retryCount++ })

        // When
        compose.onNodeWithText("Couldn't load more activity.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        // Then
        assertEquals(1, retryCount)
    }

    @Test
    fun `should say there is no activity when the list is empty`() {
        // Given / When
        setContent(ActivityListUiState.Empty)

        // Then
        compose.onNodeWithText("No activity yet.").assertIsDisplayed()
    }

    @Test
    fun `should explain the failure and offer to retry when the first page could not be loaded`() {
        // Given
        var retryCount = 0
        setContent(ActivityListUiState.Error(DomainError.ServiceUnavailable), onRetry = { retryCount++ })

        // When
        compose.onNodeWithText("Service temporarily unavailable. Please try again later.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()

        // Then
        assertEquals(1, retryCount)
    }
}
