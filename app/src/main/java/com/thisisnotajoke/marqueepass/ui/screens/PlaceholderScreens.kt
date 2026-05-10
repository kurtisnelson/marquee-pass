package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thisisnotajoke.marqueepass.data.ShowStatus

@Composable
fun SeenScreen(viewModel: ShowViewModel = viewModel()) {
    val shows by viewModel.seenShows.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    ShowListScreen(
        title = "SEEN SHOWS",
        shows = shows,
        status = ShowStatus.SEEN,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onAddShow = { viewModel.addShow(it) },
        onDeleteShow = { viewModel.deleteShow(it) }
    )
}

@Composable
fun WantToSeeScreen(viewModel: ShowViewModel = viewModel()) {
    val shows by viewModel.wantToSeeShows.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    ShowListScreen(
        title = "WISH LIST",
        shows = shows,
        status = ShowStatus.WANT_TO_SEE,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onAddShow = { viewModel.addShow(it) },
        onDeleteShow = { viewModel.deleteShow(it) }
    )
}

@Composable
fun TicketsScreen(viewModel: ShowViewModel = viewModel()) {
    val shows by viewModel.ticketedShows.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    ShowListScreen(
        title = "MY TICKETS",
        shows = shows,
        status = ShowStatus.TICKETED,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onAddShow = { viewModel.addShow(it) },
        onDeleteShow = { viewModel.deleteShow(it) }
    )
}
