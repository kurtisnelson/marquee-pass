package com.thisisnotajoke.marqueepass.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

// Custom ticket shape with notch cuts at a fixed offset from the right
class TicketShape(
    private val cutoutRadius: androidx.compose.ui.unit.Dp,
    private val cutoutOffsetFromRight: androidx.compose.ui.unit.Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
        val cutoutOffsetFromRightPx = with(density) { cutoutOffsetFromRight.toPx() }
        val notchX = size.width - cutoutOffsetFromRightPx

        val path = Path().apply {
            reset()
            moveTo(0f, 0f)
            // Line to top notch
            lineTo(notchX - cutoutRadiusPx, 0f)
            // Top notch cutout (curves downwards)
            arcTo(
                rect = Rect(
                    left = notchX - cutoutRadiusPx,
                    top = -cutoutRadiusPx,
                    right = notchX + cutoutRadiusPx,
                    bottom = cutoutRadiusPx
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Line to top-right
            lineTo(size.width, 0f)
            // Line to bottom-right
            lineTo(size.width, size.height)
            // Line to bottom notch
            lineTo(notchX + cutoutRadiusPx, size.height)
            // Bottom notch cutout (curves upwards)
            arcTo(
                rect = Rect(
                    left = notchX - cutoutRadiusPx,
                    top = size.height - cutoutRadiusPx,
                    right = notchX + cutoutRadiusPx,
                    bottom = size.height + cutoutRadiusPx
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Line to bottom-left
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}



@Composable
fun MarqueeBulbRow(
    modifier: Modifier = Modifier,
    bulbCount: Int = 18,
    color: Color = Color(0xFFFFCC00)
) {
    // Elegant infinite cycle driving a sinusoidal phase shift (Designer recommendation)
    val infiniteTransition = rememberInfiniteTransition(label = "MarqueeBulbs")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BulbPhase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until bulbCount) {
            val intensity = remember(i) {
                derivedStateOf {
                    val bulbOffset = (i.toFloat() / bulbCount.toFloat()) * (2 * Math.PI).toFloat() * 2f
                    val sinValue = Math.sin(phase.toDouble() + bulbOffset).toFloat()
                    (sinValue * 0.4f + 0.6f).coerceIn(0.2f, 1.0f)
                }
            }

            val bulbColor = color.copy(alpha = intensity.value)
            val scaleFactor = 0.85f + (intensity.value * 0.15f)

            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer(scaleX = scaleFactor, scaleY = scaleFactor)
                    .background(bulbColor, CircleShape)
            )
        }
    }
}




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
                    rating = 5,
                    notes = "An incredible epic broadway musical about Orpheus and Eurydice. Absolutely stellar design, neon lights, and amazing acoustic jazz score!"
                ),
                onUpdateShow = {},
                onEditClick = {}
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
    onDeleteShow: (Show) -> Unit,
    onUpdateShow: (Show) -> Unit,
    connectionStatus: ConnectivityStatus
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Show?>(null) }

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
                text = "MARQUEE PASS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                if (shows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "The stage is empty", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Add a show to get started.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(shows, key = { it.id }) { show ->
                            SwipeToDismissItem(
                                onDismiss = { onDeleteShow(show) }
                            ) {
                                ShowTicketItem(
                                    show = show,
                                    onUpdateShow = onUpdateShow,
                                    onEditClick = { showEditDialog = show }
                                )
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

    if (showEditDialog != null) {
        AddShowDialog(
            status = status,
            initialShow = showEditDialog,
            onDismiss = { showEditDialog = null },
            onConfirm = {
                onUpdateShow(it)
                showEditDialog = null
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
                        modifier = Modifier.padding(end = 20.dp),
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
fun ShowTicketItem(
    show: Show,
    onUpdateShow: (Show) -> Unit,
    onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = when (show.status) {
        ShowStatus.SEEN -> {
            if (isSystemDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
        }
        else -> {
            if (isSystemDark) MaterialTheme.colorScheme.secondary else Color(0xFF006064)
        }
    }

    // Dynamic, beautiful ambient color gradient containing a subtle status tint
    val cardBackgroundBrush = remember(accentColor, isSystemDark) {
        if (isSystemDark) {
            Brush.linearGradient(
                colors = listOf(
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardTop.copy(alpha = 0.98f),
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardBottom.copy(alpha = 0.98f),
                    accentColor.copy(alpha = 0.04f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFAFAFA), // Crisp white ticket surface
                    Color(0xFFF2F2F7), // Soft elevated light gray
                    accentColor.copy(alpha = 0.03f)
                )
            )
        }
    }

    val cutoutOffset = 84.dp
    val ticketShape = remember { TicketShape(cutoutRadius = 10.dp, cutoutOffsetFromRight = cutoutOffset) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(
                elevation = if (expanded) 12.dp else 4.dp,
                shape = ticketShape,
                ambientColor = accentColor.copy(alpha = 0.3f),
                spotColor = accentColor.copy(alpha = 0.3f)
            )
            .clickable { expanded = !expanded },
        shape = ticketShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.8f),
                    accentColor.copy(alpha = 0.15f),
                    accentColor.copy(alpha = 0.8f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(cardBackgroundBrush, ticketShape)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .fillMaxWidth()
        ) {
            MarqueeBulbRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                bulbCount = 18,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            // Card Front: Title details + Tear-off Stub side
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
            ) {
                // Left Column: main details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 16.dp)
                ) {

                    
                    // Show Title
                    Text(
                        text = show.title.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Theater Location
                    show.theater?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Date and Mini Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (show.status == ShowStatus.SEEN && show.date != null) {
                            val dateFormat = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }
                            val formattedDate = remember(show.date) { dateFormat.format(Date(show.date)).uppercase() }
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = accentColor
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                    }
                }
                
                // Perforated Separation line
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                ) {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        strokeWidth = 2.5f
                    )
                }
                
                // Right Column: Stub side
                Column(
                    modifier = Modifier
                        .width(cutoutOffset)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (show.status == ShowStatus.SEEN && show.rating != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = show.rating.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                    color = accentColor
                                )
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            val icon = when (show.status) {
                                ShowStatus.SEEN -> Icons.Rounded.Visibility
                                else -> Icons.Rounded.Favorite
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val stubLabel = when(show.status) {
                        ShowStatus.SEEN -> "SEEN"
                        else -> "WISH"
                    }
                    Text(
                        text = stubLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = accentColor
                    )
                }
            }
            
            // Collapsible Bottom Section
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                ) {
                    // Internal Dotted Divider
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        drawLine(
                            color = borderColor.copy(alpha = 0.3f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                            strokeWidth = 2f
                        )
                    }
                    
                    // Memo Section
                    Text(
                        text = "MEMO & REVIEWS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            val notesText = if (!show.notes.isNullOrBlank()) show.notes else "No review or personal memo added yet. Tap Edit Details below to write notes!"
                            Text(
                                text = notesText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = if (show.notes.isNullOrBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                ),
                                color = if (show.notes.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }


                    // Inline rating bar for direct updates if show is SEEN
                    if (show.status == ShowStatus.SEEN) {
                        Text(
                            text = "RATING (TAP TO UPDATE)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 14.dp)
                        ) {
                            val currentRating = show.rating ?: 0
                            for (i in 1..5) {
                                val isFilled = i <= currentRating
                                IconButton(
                                    onClick = {
                                        onUpdateShow(show.copy(rating = i))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "$i Stars",
                                        tint = if (isFilled) accentColor else accentColor.copy(alpha = 0.25f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                    

                    
                    // Expanded action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onEditClick() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EDIT DETAILS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                        
                        val actionButtonText = when (show.status) {
                            ShowStatus.SEEN -> "MOVE TO WISHLIST"
                            else -> "MARK AS SEEN"
                        }
                        
                        val actionIcon = when (show.status) {
                            ShowStatus.SEEN -> Icons.Rounded.Favorite
                            else -> Icons.Rounded.Visibility
                        }
                        
                        Button(
                            onClick = {
                                when (show.status) {
                                    ShowStatus.SEEN -> {
                                        onUpdateShow(show.copy(status = ShowStatus.WANT_TO_SEE, rating = null, date = null))
                                    }
                                    else -> {
                                        onUpdateShow(show.copy(status = ShowStatus.SEEN, rating = 5, date = System.currentTimeMillis()))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    ambientColor = accentColor,
                                    spotColor = accentColor
                                ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    actionIcon,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = actionButtonText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            MarqueeBulbRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                bulbCount = 18,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}
