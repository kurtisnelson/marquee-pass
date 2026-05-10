package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thisisnotajoke.marqueepass.data.Show
import com.thisisnotajoke.marqueepass.data.ShowStatus
import androidx.compose.ui.tooling.preview.Preview
import com.thisisnotajoke.marqueepass.ui.theme.MarqueePassTheme
import com.thisisnotajoke.marqueepass.util.ConnectivityObserver
import com.thisisnotajoke.marqueepass.util.ConnectivityStatus
import java.text.SimpleDateFormat
import java.util.*

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ShowTicketItemPreview() {
    MarqueePassTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ShowTicketItem(
                show = Show(
                    title = "Hadestown",
                    theater = "Walter Kerr Theatre",
                    status = ShowStatus.SEEN,
                    date = System.currentTimeMillis(),
                    rating = 5
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowListScreen(
    title: String,
    shows: List<Show>,
    status: ShowStatus,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAddShow: (Show) -> Unit,
    onDeleteShow: (Show) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val connectivityObserver = remember { ConnectivityObserver(context) }
    val connectionStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityStatus.Available)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Show")
            }
        },
        bottomBar = {
            if (connectionStatus != ConnectivityStatus.Available) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Offline - Changes will sync when online",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(16.dp)
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (shows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No shows yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(shows, key = { it.id }) { show ->
                            SwipeToDismissItem(
                                onDismiss = { onDeleteShow(show) }
                            ) {
                                ShowTicketItem(show = show)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddShowDialog(
            status = status,
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddShow(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun SwipeToDismissItem(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.errorContainer
            } else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        content = { content() }
    )
}

@Composable
fun ShowTicketItem(show: Show) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val accentColor = when (show.status) {
        ShowStatus.SEEN -> MaterialTheme.colorScheme.tertiary // Neon Yellow
        ShowStatus.WANT_TO_SEE -> MaterialTheme.colorScheme.secondary // Neon Cyan
        ShowStatus.TICKETED -> MaterialTheme.colorScheme.primary // Neon Pink
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = show.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                )
                show.theater?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                show.date?.let {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(it)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Divider
            Canvas(modifier = Modifier.fillMaxHeight().width(1.dp)) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Side/Rating content
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (show.status == ShowStatus.SEEN && show.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = show.rating.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    val statusText = when(show.status) {
                        ShowStatus.WANT_TO_SEE -> "WISH"
                        ShowStatus.TICKETED -> "TIX"
                        else -> ""
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }
        }
    }
}
