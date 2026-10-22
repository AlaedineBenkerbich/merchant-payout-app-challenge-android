package com.example.androidinterview.ui.activity

import com.example.androidinterview.domain.model.ActivityGroup
import com.example.androidinterview.domain.model.DomainError

sealed interface ActivityListUiState {
    data object Loading : ActivityListUiState
    data class Error(val error: DomainError) : ActivityListUiState
    data object Empty : ActivityListUiState
    data class Content(val groups: List<ActivityGroup>, val footer: PagingFooter) : ActivityListUiState
}

/** What the bottom of the list shows, which also decides whether another page may be requested. */
sealed interface PagingFooter {
    /** More pages exist and none is being fetched. */
    data object Idle : PagingFooter
    data object Loading : PagingFooter
    data class Error(val error: DomainError) : PagingFooter
    data object EndReached : PagingFooter
}
