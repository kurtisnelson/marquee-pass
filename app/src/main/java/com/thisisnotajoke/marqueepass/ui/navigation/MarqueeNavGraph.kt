package com.thisisnotajoke.marqueepass.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material3.Text
import com.thisisnotajoke.marqueepass.ui.screens.SeenScreen
import com.thisisnotajoke.marqueepass.ui.screens.WantToSeeScreen
import com.thisisnotajoke.marqueepass.ui.screens.TicketsScreen
import com.thisisnotajoke.marqueepass.ui.screens.ProfileScreen

@Composable
fun MarqueeNavGraph(backStack: SnapshotStateList<Route>) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Route.Seen -> NavEntry(key) { SeenScreen() }
                is Route.WantToSee -> NavEntry(key) { WantToSeeScreen() }
                is Route.Tickets -> NavEntry(key) { TicketsScreen() }
                is Route.Profile -> NavEntry(key) { ProfileScreen() }
            }
        }
    )
}
