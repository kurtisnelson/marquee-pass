package com.thisisnotajoke.marqueepass.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Seen : Route
    
    @Serializable
    data object WantToSee : Route
    
    @Serializable
    data object Tickets : Route

    @Serializable
    data object Profile : Route
}
