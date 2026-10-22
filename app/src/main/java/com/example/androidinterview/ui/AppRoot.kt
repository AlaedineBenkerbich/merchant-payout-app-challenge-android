package com.example.androidinterview.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.androidinterview.ui.activity.ActivityListRoute
import com.example.androidinterview.ui.home.HomeRoute
import com.example.androidinterview.ui.navigation.ActivityDestination
import com.example.androidinterview.ui.navigation.HomeDestination
import com.example.androidinterview.ui.navigation.PayoutDestination
import com.example.androidinterview.ui.navigation.TopLevelTab
import com.example.androidinterview.ui.payout.PayoutRoute

@Composable
fun AppRoot(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isActivityModalOpen = destination?.hasRoute<ActivityDestination>() == true
    // The activity list is presented over Home, so Home stays the selected tab while it is open.
    val selectedTab = if (destination?.hasRoute<PayoutDestination>() == true) {
        TopLevelTab.Payouts
    } else {
        TopLevelTab.Home
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // A modal covers the whole screen, tab bar included.
            if (!isActivityModalOpen) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                            label = { Text(text = stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeDestination>(
                // Home stays put underneath the modal instead of fading while the modal slides over it.
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
            ) {
                HomeRoute(
                    viewModel = viewModel(factory = viewModelFactory),
                    // Single top: a fast double tap must not stack two modals.
                    onShowMore = { navController.navigate(ActivityDestination) { launchSingleTop = true } },
                )
            }
            composable<PayoutDestination> {
                PayoutRoute(viewModel = viewModel(factory = viewModelFactory))
            }
            // A full-screen modal: it slides up over Home and is dismissed with "Done" or Back.
            composable<ActivityDestination>(
                enterTransition = { slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) },
                popExitTransition = { slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) },
            ) {
                ActivityListRoute(
                    viewModel = viewModel(factory = viewModelFactory),
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Switches tab while keeping each tab's state (form, scroll position) when coming back to it. */
private fun NavController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
