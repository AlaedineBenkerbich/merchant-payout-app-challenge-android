package com.example.androidinterview.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.androidinterview.R
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data object PayoutDestination

/** The full activity list, presented as a full-screen modal over the home screen. */
@Serializable
data object ActivityDestination

/** The two tabs of the bottom navigation bar. */
enum class TopLevelTab(
    val destination: Any,
    val icon: ImageVector,
    @param:StringRes val labelRes: Int,
) {
    Home(HomeDestination, Icons.Filled.Home, R.string.nav_home),
    Payouts(PayoutDestination, Icons.AutoMirrored.Filled.Send, R.string.nav_payouts),
}
