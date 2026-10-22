package com.example.androidinterview.ui.activity

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldLoadMoreTest {

    @Test
    fun `should not load more when no row is visible yet`() {
        // Given
        val lastVisibleIndex: Int? = null

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 30, threshold = 4)

        // Then
        assertFalse(shouldLoad)
    }

    @Test
    fun `should not load more when many rows remain below the last visible one`() {
        // Given
        val lastVisibleIndex = 5

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 30, threshold = 4)

        // Then
        assertFalse(shouldLoad)
    }

    @Test
    fun `should not load more just outside the threshold`() {
        // Given
        val lastVisibleIndex = 25

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 30, threshold = 4)

        // Then
        assertFalse(shouldLoad)
    }

    @Test
    fun `should load more when the last visible row reaches the threshold`() {
        // Given
        val lastVisibleIndex = 26

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 30, threshold = 4)

        // Then
        assertTrue(shouldLoad)
    }

    @Test
    fun `should load more when the last row of the list is visible`() {
        // Given
        val lastVisibleIndex = 29

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 30, threshold = 4)

        // Then
        assertTrue(shouldLoad)
    }

    @Test
    fun `should load more when the whole list fits on screen`() {
        // Given: three rows, all visible, fewer than the threshold.
        val lastVisibleIndex = 2

        // When
        val shouldLoad = shouldLoadMore(lastVisibleIndex, totalItemsCount = 3, threshold = 4)

        // Then
        assertTrue(shouldLoad)
    }
}
