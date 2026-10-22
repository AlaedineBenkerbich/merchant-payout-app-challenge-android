package com.example.androidinterview.ui.activity

import com.example.androidinterview.domain.model.ActivityDay
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.usecase.GetActivityPageUseCase
import com.example.androidinterview.domain.usecase.GroupActivityByDateUseCase
import com.example.androidinterview.testing.FakeMerchantRepository
import com.example.androidinterview.testing.activityItem
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityListViewModelTest {

    private val merchants = FakeMerchantRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ActivityListViewModel(
        getActivityPage = GetActivityPageUseCase(merchants),
        groupByDate = GroupActivityByDateUseCase { clock },
    )

    private fun page(vararg ids: String, nextCursor: String? = null) = ActivityPage(
        items = ids.map { activityItem(id = it) },
        nextCursor = nextCursor,
        hasMore = nextCursor != null,
    )

    private fun ActivityListViewModel.loadedIds(): List<String> {
        val content = assertIs<ActivityListUiState.Content>(uiState.value)
        return content.groups.flatMap { group -> group.items.map { it.id } }
    }

    private fun ActivityListViewModel.footer(): PagingFooter =
        assertIs<ActivityListUiState.Content>(uiState.value).footer

    private fun ActivityListViewModel.firstDay(): ActivityDay =
        assertIs<ActivityListUiState.Content>(uiState.value).groups.first().day

    @Test
    fun `should show loading while the first page is being fetched`() {
        // Given
        merchants.activityGate = CompletableDeferred()
        merchants.enqueuePage(null, Outcome.Success(page("a")))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(ActivityListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `should show the first page when it loads and more pages exist`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", "b", nextCursor = "b")))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(listOf("a", "b"), viewModel.loadedIds())
        assertEquals(PagingFooter.Idle, viewModel.footer())
    }

    @Test
    fun `should request the following page with the cursor when more items are needed`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", "b", nextCursor = "b")))
        merchants.enqueuePage("b", Outcome.Success(page("c", "d")))
        val viewModel = viewModel()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf(null, "b"), merchants.requestedCursors)
        assertEquals(listOf("a", "b", "c", "d"), viewModel.loadedIds())
        assertEquals(PagingFooter.EndReached, viewModel.footer())
    }

    @Test
    fun `should not request a page twice when more items are needed while one is loading`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", nextCursor = "a")))
        merchants.enqueuePage("a", Outcome.Success(page("b")))
        val viewModel = viewModel()
        merchants.activityGate = CompletableDeferred()

        // When
        viewModel.onLoadMore()
        viewModel.onLoadMore()
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf(null, "a"), merchants.requestedCursors)
        assertEquals(PagingFooter.Loading, viewModel.footer())
    }

    @Test
    fun `should ignore items already shown when pages overlap`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", "b", nextCursor = "b")))
        merchants.enqueuePage("b", Outcome.Success(page("b", "c")))
        val viewModel = viewModel()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf("a", "b", "c"), viewModel.loadedIds())
    }

    @Test
    fun `should stop asking for pages when the last page has been loaded`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a")))
        val viewModel = viewModel()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf<String?>(null), merchants.requestedCursors)
        assertEquals(PagingFooter.EndReached, viewModel.footer())
    }

    @Test
    fun `should stop paging when a page brings nothing new despite announcing more`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", nextCursor = "a")))
        merchants.enqueuePage("a", Outcome.Success(page("a", nextCursor = "a")))
        val viewModel = viewModel()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(PagingFooter.EndReached, viewModel.footer())
    }

    @Test
    fun `should keep the loaded items and show a footer error when the following page fails`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", nextCursor = "a")))
        merchants.enqueuePage("a", Outcome.Failure(DomainError.NoConnection))
        val viewModel = viewModel()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf("a"), viewModel.loadedIds())
        assertEquals(PagingFooter.Error(DomainError.NoConnection), viewModel.footer())
    }

    @Test
    fun `should not retry on its own when the footer is in error`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", nextCursor = "a")))
        merchants.enqueuePage("a", Outcome.Failure(DomainError.NoConnection))
        val viewModel = viewModel()
        viewModel.onLoadMore()

        // When
        viewModel.onLoadMore()

        // Then
        assertEquals(listOf(null, "a"), merchants.requestedCursors)
    }

    @Test
    fun `should load the failed page again when the merchant retries`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page("a", nextCursor = "a")))
        merchants.enqueuePage("a", Outcome.Failure(DomainError.NoConnection), Outcome.Success(page("b")))
        val viewModel = viewModel()
        viewModel.onLoadMore()

        // When
        viewModel.onRetryLoadMore()

        // Then
        assertEquals(listOf("a", "b"), viewModel.loadedIds())
        assertEquals(PagingFooter.EndReached, viewModel.footer())
    }

    @Test
    fun `should show the error when the first page fails and recover when retried`() {
        // Given
        merchants.enqueuePage(null, Outcome.Failure(DomainError.ServiceUnavailable), Outcome.Success(page("a")))
        val viewModel = viewModel()
        assertEquals(ActivityListUiState.Error(DomainError.ServiceUnavailable), viewModel.uiState.value)

        // When
        viewModel.onRetry()

        // Then
        assertEquals(listOf("a"), viewModel.loadedIds())
    }

    @Test
    fun `should relabel the days when asked to group again after midnight`() {
        // Given: the only item happened on 20 October, which is "today" when the page arrives.
        var now = Instant.parse("2026-10-20T12:00:00Z")
        val itemOfTheMorning = activityItem(id = "a", occurredAt = Instant.parse("2026-10-20T09:00:00Z"))
        merchants.enqueuePage(null, Outcome.Success(ActivityPage(listOf(itemOfTheMorning), null, false)))
        val viewModel = ActivityListViewModel(
            getActivityPage = GetActivityPageUseCase(merchants),
            groupByDate = GroupActivityByDateUseCase { Clock.fixed(now, ZoneOffset.UTC) },
        )
        assertEquals(ActivityDay.Today, viewModel.firstDay())

        // When: midnight passes while the screen is in the background.
        now = Instant.parse("2026-10-21T00:30:00Z")
        viewModel.onRegroupRequested()

        // Then
        assertEquals(ActivityDay.Yesterday, viewModel.firstDay())
    }

    @Test
    fun `should do nothing when asked to group again while there is nothing to show`() {
        // Given
        merchants.activityGate = CompletableDeferred()
        merchants.enqueuePage(null, Outcome.Success(page("a")))
        val viewModel = viewModel()

        // When
        viewModel.onRegroupRequested()

        // Then
        assertEquals(ActivityListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `should show the empty state when there is no activity`() {
        // Given
        merchants.enqueuePage(null, Outcome.Success(page()))

        // When
        val viewModel = viewModel()

        // Then
        assertEquals(ActivityListUiState.Empty, viewModel.uiState.value)
    }
}
