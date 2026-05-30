package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zacsweers.metrox.viewmodel.metroViewModel
import com.thisisnotajoke.marqueepass.data.ShowStatus

@Composable
fun SeenScreen(viewModel: ShowViewModel = metroViewModel()) {
    val shows by viewModel.seenShows.collectAsState(initial = emptyList())
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    ShowListScreen(
        title = "SEEN SHOWS",
        shows = shows,
        status = ShowStatus.SEEN,
        connectionStatus = connectionStatus,
        onAddShow = { viewModel.addShow(it) },
        onDeleteShow = { viewModel.deleteShow(it) },
        onUpdateShow = { viewModel.updateShow(it) }
    )
}

@Composable
fun WantToSeeScreen(viewModel: ShowViewModel = metroViewModel()) {
    val shows by viewModel.wantToSeeShows.collectAsState(initial = emptyList())
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    ShowListScreen(
        title = "WISH LIST",
        shows = shows,
        status = ShowStatus.WANT_TO_SEE,
        connectionStatus = connectionStatus,
        onAddShow = { viewModel.addShow(it) },
        onDeleteShow = { viewModel.deleteShow(it) },
        onUpdateShow = { viewModel.updateShow(it) }
    )
}
