package com.thisisnotajoke.marqueepass.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.thisisnotajoke.marqueepass.R
import com.thisisnotajoke.marqueepass.ui.theme.NeonCyan
import com.thisisnotajoke.marqueepass.ui.theme.NeonPink
import com.thisisnotajoke.marqueepass.ui.theme.PlaybillYellow
import kotlinx.coroutines.launch

// Custom horizontal ticket shape with perforation cutouts on left and right edges
class HorizontalTicketShape(
    private val cutoutRadius: androidx.compose.ui.unit.Dp,
    private val cutoutOffsetFromBottom: androidx.compose.ui.unit.Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
        val cutoutOffsetFromBottomPx = with(density) { cutoutOffsetFromBottom.toPx() }
        val notchY = size.height - cutoutOffsetFromBottomPx

        val path = Path().apply {
            reset()
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            // Line to right notch
            lineTo(size.width, notchY - cutoutRadiusPx)
            // Right notch cutout (curves inwards left)
            arcTo(
                rect = Rect(
                    left = size.width - cutoutRadiusPx,
                    top = notchY - cutoutRadiusPx,
                    right = size.width + cutoutRadiusPx,
                    bottom = notchY + cutoutRadiusPx
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            // Line to left notch
            lineTo(0f, notchY + cutoutRadiusPx)
            // Left notch cutout (curves inwards right)
            arcTo(
                rect = Rect(
                    left = -cutoutRadiusPx,
                    top = notchY - cutoutRadiusPx,
                    right = cutoutRadiusPx,
                    bottom = notchY + cutoutRadiusPx
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun MarqueeHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(com.thisisnotajoke.marqueepass.ui.theme.PlaybillYellow)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "MARQUEE PASS",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = com.thisisnotajoke.marqueepass.ui.theme.PlaybillFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                letterSpacing = 4.sp,
                fontSize = 22.sp
            )
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ShowViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    // Live local/synced database statistics for the stats grid
    val seenShows by viewModel.seenShows.collectAsState(initial = emptyList())
    val wishlistShows by viewModel.wantToSeeShows.collectAsState(initial = emptyList())

    val seenCount = seenShows.size
    val wishCount = wishlistShows.size

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Title
                Text(
                    text = "MY PROFILE",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    textAlign = TextAlign.Start
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonPink,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentUser,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "ProfileCardTransition",
                        modifier = Modifier.weight(1f)
                    ) { user ->
                        if (user == null || user.isAnonymous) {
                            GuestProfileCard(
                                seenCount = seenCount,
                                wishCount = wishCount,
                                onGoogleSignInClick = {
                                    scope.launch {
                                        try {
                                            val webClientId = context.getString(R.string.default_web_client_id)
                                            if (webClientId == "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com" || webClientId.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Web Client ID not configured. Please add your client ID to strings.xml.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }

                                            val googleIdOption = GetGoogleIdOption.Builder()
                                                .setFilterByAuthorizedAccounts(false)
                                                .setServerClientId(webClientId)
                                                .build()

                                            val request = GetCredentialRequest.Builder()
                                                .addCredentialOption(googleIdOption)
                                                .build()

                                            val result = credentialManager.getCredential(context, request)
                                            val credential = result.credential

                                            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                                val idToken = googleIdTokenCredential.idToken
                                                
                                                isLoading = true
                                                viewModel.handleGoogleSignIn(
                                                    idToken = idToken,
                                                    onSuccess = {
                                                        isLoading = false
                                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { error ->
                                                        isLoading = false
                                                        Toast.makeText(context, "Authentication failed: $error", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            }
                                        } catch (e: GetCredentialException) {
                                            Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        } else {
                            SyncedProfileCard(
                                email = user.email ?: "",
                                displayName = user.displayName ?: "User",
                                photoUrl = user.photoUrl?.toString(),
                                seenCount = seenCount,
                                wishCount = wishCount,
                                onSignOutClick = {
                                    viewModel.signOut(
                                        onSuccess = {
                                            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuestProfileCard(
    seenCount: Int,
    wishCount: Int,
    onGoogleSignInClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    
    // Perforated cutout offset is exactly 112.dp from the bottom
    val cutoutOffset = 112.dp
    val ticketShape = remember { HorizontalTicketShape(cutoutRadius = 12.dp, cutoutOffsetFromBottom = cutoutOffset) }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val guestBgBrush = remember(isSystemDark) {
        if (isSystemDark) {
            Brush.linearGradient(
                colors = listOf(
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardTop,
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardBottom,
                    NeonPink.copy(alpha = 0.03f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFAFAFA),
                    Color(0xFFF2F2F7),
                    NeonPink.copy(alpha = 0.02f)
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(
                elevation = 16.dp,
                shape = ticketShape,
                ambientColor = NeonPink.copy(alpha = 0.25f),
                spotColor = NeonPink.copy(alpha = 0.25f)
            ),
        shape = ticketShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(listOf(NeonPink, NeonPink.copy(alpha = 0.2f), NeonPink))
        )
    ) {
        Column(
            modifier = Modifier
                .background(guestBgBrush, ticketShape)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MarqueeHeader()

            // Main Card Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tag Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "GUEST PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = NeonPink
                    )
                    Text(
                        text = "№ LOCAL-USER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OFFLINE PROFILE STATS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(label = "SEEN", value = seenCount.toString(), color = PlaybillYellow, modifier = Modifier.weight(1f))
                    StatItem(label = "WISH", value = wishCount.toString(), color = NeonCyan, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sign in to secure your profile and sync instantly across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dashed Perforated Divider connecting cutouts
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                    strokeWidth = 2.5f
                )
            }

            // Bottom portion (CTA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ENABLE CLOUD SYNC",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Google button
                Button(
                    onClick = onGoogleSignInClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(46.dp)
                        .border(1.2.dp, Brush.horizontalGradient(listOf(NeonPink, NeonCyan)), RoundedCornerShape(23.dp))
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SIGN IN WITH GOOGLE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncedProfileCard(
    email: String,
    displayName: String,
    photoUrl: String?,
    seenCount: Int,
    wishCount: Int,
    onSignOutClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    
    // Perforated cutout offset is exactly 112.dp from the bottom
    val cutoutOffset = 112.dp
    val ticketShape = remember { HorizontalTicketShape(cutoutRadius = 12.dp, cutoutOffsetFromBottom = cutoutOffset) }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val profileBgBrush = remember(isSystemDark) {
        if (isSystemDark) {
            Brush.linearGradient(
                colors = listOf(
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardTop,
                    com.thisisnotajoke.marqueepass.ui.theme.TicketCardBottom,
                    com.thisisnotajoke.marqueepass.ui.theme.AmbientDeepViolet
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFAFAFA),
                    Color(0xFFF2F2F7),
                    Color(0xFFF3E5F5)
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(
                elevation = 20.dp,
                shape = ticketShape,
                ambientColor = NeonCyan.copy(alpha = 0.3f),
                spotColor = NeonCyan.copy(alpha = 0.3f)
            ),
        shape = ticketShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                colors = listOf(NeonPink, NeonCyan, PlaybillYellow, NeonPink)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .background(profileBgBrush, ticketShape)
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MarqueeHeader()

            // Main Card Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tag Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SYNCED PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = NeonCyan
                    )
                    Text(
                        text = "№ PROFILE-${email.hashCode().toString().takeLast(6).padStart(6, '0')}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Circular profile picture with sweep gradient glowing ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .border(
                            2.5.dp, 
                            Brush.sweepGradient(listOf(NeonPink, NeonCyan, PlaybillYellow, NeonPink)), 
                            CircleShape
                        )
                        .padding(4.dp)
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            color = Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(label = "SEEN", value = seenCount.toString(), color = PlaybillYellow, modifier = Modifier.weight(1f))
                    StatItem(label = "WISH", value = wishCount.toString(), color = NeonCyan, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dashed Perforated Divider connecting cutouts
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                    strokeWidth = 2.5f
                )
            }

            // Bottom portion (Sync & Logout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = PlaybillYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLOUD SYNC ENABLED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlaybillYellow,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Elegant translucent Sign-Out Button
                Button(
                    onClick = onSignOutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(38.dp)
                        .border(1.dp, NeonPink, RoundedCornerShape(19.dp))
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIGN OUT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

// Gorgeous capsule stats component
@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    Surface(
        color = if (isSystemDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = if (isSystemDark) 0.3f else 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            val icon = when (label) {
                "SEEN" -> Icons.Rounded.Star
                "WISH" -> Icons.Rounded.Favorite
                "TIX", "TICKETS" -> Icons.Rounded.ConfirmationNumber
                else -> Icons.Rounded.Star
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ),
                    color = color
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}


