package com.example.androidinterview.ui.activity

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidinterview.R
import com.example.androidinterview.domain.model.ActivityDay
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.ActivityStatus
import com.example.androidinterview.domain.model.ActivityType
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.ui.common.ActivityDateFormatter
import com.example.androidinterview.ui.common.AmountText
import com.example.androidinterview.ui.common.ErrorContent
import com.example.androidinterview.ui.common.LoadingContent
import com.example.androidinterview.ui.common.toMessageRes
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme

/** How many rows before the end of the list the next page is requested. */
private const val LOAD_MORE_THRESHOLD = 4

@Composable
fun ActivityListRoute(
    viewModel: ActivityListViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onRegroupRequested() }
    ActivityListScreen(
        state = state,
        onDone = onDone,
        onRetry = viewModel::onRetry,
        onLoadMore = viewModel::onLoadMore,
        onRetryLoadMore = viewModel::onRetryLoadMore,
        modifier = modifier,
    )
}

@Composable
fun ActivityListScreen(
    state: ActivityListUiState,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            ActivityListHeader(onDone = onDone)
            HorizontalDivider()
            when (state) {
                ActivityListUiState.Loading -> LoadingContent(description = stringResource(R.string.activity_loading))
                is ActivityListUiState.Error -> ErrorContent(
                    message = stringResource(state.error.toMessageRes()),
                    onRetry = onRetry,
                )
                ActivityListUiState.Empty -> EmptyContent()
                is ActivityListUiState.Content -> ActivityGroupedList(
                    content = state,
                    onLoadMore = onLoadMore,
                    onRetryLoadMore = onRetryLoadMore,
                )
            }
        }
    }
}

@Composable
private fun ActivityListHeader(onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.activity_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        TextButton(onClick = onDone) {
            Text(text = stringResource(R.string.activity_done))
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.activity_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityGroupedList(
    content: ActivityListUiState.Content,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val isNearEnd by remember { derivedStateOf { listState.isNearEnd() } }

    // Also keyed on the footer: when a page arrives and the end is still in view, ask for the next one.
    LaunchedEffect(isNearEnd, content.footer) {
        if (isNearEnd) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        content.groups.forEach { group ->
            stickyHeader(key = "header-${group.day.key()}") {
                GroupHeader(day = group.day)
            }
            items(items = group.items, key = { it.id }) { item ->
                ActivityRow(item = item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        item(key = "footer") {
            PagingFooterRow(footer = content.footer, onRetry = onRetryLoadMore)
        }
    }
}

private fun LazyListState.isNearEnd(): Boolean =
    shouldLoadMore(
        lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index,
        totalItemsCount = layoutInfo.totalItemsCount,
    )

/** True once few enough rows are left below the last visible one that the next page should be requested. */
internal fun shouldLoadMore(
    lastVisibleIndex: Int?,
    totalItemsCount: Int,
    threshold: Int = LOAD_MORE_THRESHOLD,
): Boolean = lastVisibleIndex != null && lastVisibleIndex >= totalItemsCount - threshold

private fun ActivityDay.key(): String = when (this) {
    ActivityDay.Today -> "today"
    ActivityDay.Yesterday -> "yesterday"
    is ActivityDay.On -> date.toString()
}

@Composable
private fun GroupHeader(day: ActivityDay) {
    val label = when (day) {
        ActivityDay.Today -> stringResource(R.string.activity_today)
        ActivityDay.Yesterday -> stringResource(R.string.activity_yesterday)
        is ActivityDay.On -> ActivityDateFormatter.format(day.date)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            // Read as one item, e.g. "Deposit, Payment received, 23 Jan 2026, £727.38, Completed".
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(item.type.labelRes()),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = ActivityDateFormatter.format(item.occurredAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AmountText(money = item.amount, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(item.status.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PagingFooterRow(footer: PagingFooter, onRetry: () -> Unit) {
    when (footer) {
        PagingFooter.Loading -> {
            val loadingText = stringResource(R.string.activity_loading_more)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Text(
                    text = loadingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is PagingFooter.Error -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.activity_load_more_failed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.action_retry))
            }
        }

        PagingFooter.Idle, PagingFooter.EndReached -> Unit
    }
}

@StringRes
private fun ActivityType.labelRes(): Int = when (this) {
    ActivityType.PAYOUT -> R.string.activity_type_payout
    ActivityType.DEPOSIT -> R.string.activity_type_deposit
    ActivityType.REFUND -> R.string.activity_type_refund
    ActivityType.FEE -> R.string.activity_type_fee
    ActivityType.OTHER -> R.string.activity_type_other
}

@StringRes
private fun ActivityStatus.labelRes(): Int = when (this) {
    ActivityStatus.COMPLETED -> R.string.status_completed
    ActivityStatus.PENDING -> R.string.status_pending
    ActivityStatus.PROCESSING -> R.string.status_processing
    ActivityStatus.FAILED -> R.string.status_failed
    ActivityStatus.UNKNOWN -> R.string.status_unknown
}

@Preview(showBackground = true)
@Composable
private fun ActivityListContentPreview() {
    AndroidInterviewTheme {
        ActivityListScreen(
            state = ActivityListUiState.Content(PreviewData.activityGroups, PagingFooter.Loading),
            onDone = {},
            onRetry = {},
            onLoadMore = {},
            onRetryLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityListEmptyPreview() {
    AndroidInterviewTheme {
        ActivityListScreen(
            state = ActivityListUiState.Empty,
            onDone = {},
            onRetry = {},
            onLoadMore = {},
            onRetryLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivityListErrorPreview() {
    AndroidInterviewTheme {
        ActivityListScreen(
            state = ActivityListUiState.Error(DomainError.ServiceUnavailable),
            onDone = {},
            onRetry = {},
            onLoadMore = {},
            onRetryLoadMore = {},
        )
    }
}
