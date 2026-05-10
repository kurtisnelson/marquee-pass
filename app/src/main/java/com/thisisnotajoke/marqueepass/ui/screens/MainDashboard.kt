package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.thisisnotajoke.marqueepass.ui.navigation.MarqueeNavGraph
import com.thisisnotajoke.marqueepass.ui.navigation.Route

@Composable
fun MainDashboard() {
    val backStack = remember { mutableStateListOf<Route>(Route.Seen) }
    val currentRoute = backStack.lastOrNull() ?: Route.Seen
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = with(adaptiveInfo.windowSizeClass) {
        when {
            windowWidthSizeClass == WindowWidthSizeClass.EXPANDED -> NavigationSuiteType.NavigationDrawer
            windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> NavigationSuiteType.NavigationRail
            else -> NavigationSuiteType.NavigationBar
        }
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        navigationSuiteItems = {
            item(
                selected = currentRoute == Route.Seen,
                onClick = {
                    if (currentRoute != Route.Seen) {
                        backStack.clear()
                        backStack.add(Route.Seen)
                    }
                },
                icon = { Icon(Icons.Rounded.Visibility, contentDescription = "Seen") },
                label = { Text("Seen") }
            )
            item(
                selected = currentRoute == Route.WantToSee,
                onClick = {
                    if (currentRoute != Route.WantToSee) {
                        backStack.clear()
                        backStack.add(Route.WantToSee)
                    }
                },
                icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Want to See") },
                label = { Text("Wishlist") }
            )
            item(
                selected = currentRoute == Route.Tickets,
                onClick = {
                    if (currentRoute != Route.Tickets) {
                        backStack.clear()
                        backStack.add(Route.Tickets)
                    }
                },
                icon = { Icon(Icons.Rounded.ConfirmationNumber, contentDescription = "Tickets") },
                label = { Text("Tickets") }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                MarqueeNavGraph(
                    backStack = backStack
                )
            }
        }
    }
}
