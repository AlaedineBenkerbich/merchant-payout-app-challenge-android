package com.example.androidinterview.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidinterview.R
import com.example.androidinterview.domain.model.ActivityItem
import com.example.androidinterview.domain.model.DomainError
import com.example.androidinterview.domain.model.MerchantOverview
import com.example.androidinterview.domain.model.Money
import com.example.androidinterview.ui.common.AmountText
import com.example.androidinterview.ui.common.ErrorContent
import com.example.androidinterview.ui.common.LoadingContent
import com.example.androidinterview.ui.common.MoneyFormatter
import com.example.androidinterview.ui.common.ScreenTitle
import com.example.androidinterview.ui.common.SectionTitle
import com.example.androidinterview.ui.common.toMessageRes
import com.example.androidinterview.ui.preview.PreviewData
import com.example.androidinterview.ui.theme.AndroidInterviewTheme

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onShowMore = onShowMore,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        ScreenTitle(text = stringResource(R.string.home_title))
        when (state) {
            HomeUiState.Loading -> LoadingContent(description = stringResource(R.string.home_loading))
            is HomeUiState.Error -> ErrorContent(
                message = stringResource(state.error.toMessageRes()),
                onRetry = onRetry,
            )
            is HomeUiState.Success -> HomeContent(overview = state.overview, onShowMore = onShowMore)
        }
    }
}

@Composable
private fun HomeContent(overview: MerchantOverview, onShowMore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BalanceCard(available = overview.availableBalance, pending = overview.pendingBalance)
        RecentActivity(items = overview.recentActivity, onShowMore = onShowMore)
    }
}

@Composable
private fun BalanceCard(available: Money, pending: Money) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(text = stringResource(R.string.home_account_balance))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BalanceFigure(
                    label = stringResource(R.string.home_available),
                    amount = available,
                    modifier = Modifier.weight(1f),
                )
                BalanceFigure(
                    label = stringResource(R.string.home_pending),
                    amount = pending,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BalanceFigure(label: String, amount: Money, modifier: Modifier = Modifier) {
    // Read as one item, e.g. "Available, £5,000.00".
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = MoneyFormatter.format(amount),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun RecentActivity(items: List<ActivityItem>, onShowMore: () -> Unit) {
    Column {
        SectionTitle(text = stringResource(R.string.home_recent_activity))
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_recent_activity),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        } else {
            items.forEach { item ->
                CompactActivityRow(item)
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onShowMore,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Text(text = stringResource(R.string.home_show_more))
        }
    }
}

/** A single line: what happened, and how much. */
@Composable
private fun CompactActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        AmountText(money = item.amount)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenSuccessPreview() {
    AndroidInterviewTheme {
        HomeScreen(state = HomeUiState.Success(PreviewData.overview), onRetry = {}, onShowMore = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    AndroidInterviewTheme {
        HomeScreen(state = HomeUiState.Loading, onRetry = {}, onShowMore = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    AndroidInterviewTheme {
        HomeScreen(state = HomeUiState.Error(DomainError.NoConnection), onRetry = {}, onShowMore = {})
    }
}
