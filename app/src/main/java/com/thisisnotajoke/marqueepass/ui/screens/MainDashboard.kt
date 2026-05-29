package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.AccountCircle
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

    val ambientGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF13091C), // Deep midnight violet ambient glow
            Color(0xFF070C15), // Smooth slate dark blue
            Color(0xFF080808)  // Obsidian black base
        )
    )

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
                selected = currentRoute == Route.Profile,
                onClick = {
                    if (currentRoute != Route.Profile) {
                        backStack.clear()
                        backStack.add(Route.Profile)
                    }
                },
                icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile") },
                label = { Text("Profile") }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ambientGradient)
                    .padding(innerPadding)
            ) {
                MarqueeNavGraph(
                    backStack = backStack
                )
            }
        }
    }
}
