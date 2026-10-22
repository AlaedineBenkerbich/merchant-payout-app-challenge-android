package com.example.androidinterview.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityPage
import com.example.androidinterview.domain.model.Outcome
import com.example.androidinterview.domain.usecase.GetActivityPageUseCase
import com.example.androidinterview.domain.usecase.GroupActivityByDateUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads the activity feed one cursor-based page at a time. */
class ActivityListViewModel(
    private val getActivityPage: GetActivityPageUseCase,
    private val groupByDate: GroupActivityByDateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityListUiState>(ActivityListUiState.Loading)
    val uiState: StateFlow<ActivityListUiState> = _uiState.asStateFlow()

    private var items: List<ActivityItem> = emptyList()
    private var nextCursor: String? = null
    private var loadJob: Job? = null

    init {
        loadFirstPage()
    }

    /** Retries after the first page failed to load. */
    fun onRetry() {
        loadFirstPage()
    }

    /** Called when the merchant scrolls close to the end of the list. */
    fun onLoadMore() {
        // The footer is the single source of truth: only an idle footer allows a new request,
        // so a page is never requested twice while one is already in flight.
        if (currentFooter() == PagingFooter.Idle) loadNextPage()
    }

    /** Retries after a following page failed to load. */
    fun onRetryLoadMore() {
        if (currentFooter() is PagingFooter.Error) loadNextPage()
    }

    /**
     * Groups the days again without fetching anything. "Today" and "Yesterday" are relative, so
     * they go stale after midnight or a time zone change while the screen was in the background.
     */
    fun onRegroupRequested() {
        val content = _uiState.value as? ActivityListUiState.Content ?: return
        _uiState.value = content.copy(groups = groupByDate(items))
    }

    private fun currentFooter(): PagingFooter? = (_uiState.value as? ActivityListUiState.Content)?.footer

    private fun loadFirstPage() {
        loadJob?.cancel()
        _uiState.value = ActivityListUiState.Loading
        loadJob = viewModelScope.launch {
            when (val outcome = getActivityPage(cursor = null)) {
                is Outcome.Success -> showPage(outcome.value, replacingItems = true)
                is Outcome.Failure -> _uiState.value = ActivityListUiState.Error(outcome.error)
            }
        }
    }

    private fun loadNextPage() {
        val cursor = nextCursor ?: return
        showFooter(PagingFooter.Loading)
        loadJob = viewModelScope.launch {
            when (val outcome = getActivityPage(cursor)) {
                is Outcome.Success -> showPage(outcome.value, replacingItems = false)
                is Outcome.Failure -> showFooter(PagingFooter.Error(outcome.error))
            }
        }
    }

    private fun showPage(page: ActivityPage, replacingItems: Boolean) {
        val knownIds = if (replacingItems) emptySet() else items.mapTo(HashSet()) { it.id }
        val newItems = page.items.filterNot { it.id in knownIds }
        items = if (replacingItems) newItems else items + newItems
        nextCursor = page.nextCursor

        // A page that brings nothing new would make the list ask for the same page forever.
        val hasMore = page.hasMore && page.nextCursor != null && newItems.isNotEmpty()
        _uiState.value = if (items.isEmpty()) {
            ActivityListUiState.Empty
        } else {
            ActivityListUiState.Content(
                groups = groupByDate(items),
                footer = if (hasMore) PagingFooter.Idle else PagingFooter.EndReached,
            )
        }
    }

    private fun showFooter(footer: PagingFooter) {
        val content = _uiState.value as? ActivityListUiState.Content ?: return
        _uiState.value = content.copy(footer = footer)
    }
}
