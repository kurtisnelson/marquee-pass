package com.thisisnotajoke.marqueepass.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.thisisnotajoke.marqueepass.ui.theme.NeonYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ShowViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    val accentGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(NeonPink, NeonCyan, NeonYellow)
        )
    }

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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Screen Title
                Text(
                    text = "MY PASS",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
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
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ProfileCardTransition",
                        modifier = Modifier.weight(1f)
                    ) { user ->
                        if (user == null || user.isAnonymous) {
                            // Guest Ticket UI
                            GuestTicketStub(
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
                                                        Toast.makeText(context, "Welcome to MarqueePass V.I.P!", Toast.LENGTH_SHORT).show()
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
                            // VIP Ticket UI
                            VIPTicketStub(
                                email = user.email ?: "",
                                displayName = user.displayName ?: "VIP Member",
                                photoUrl = user.photoUrl?.toString(),
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
fun GuestTicketStub(onGoogleSignInClick: () -> Unit) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Ticket Body
            Text(
                text = "GUEST TICKET",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonPink,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Keep your broadway passes stored locally on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dashed Divider
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
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tear-Off Stub (Call to Action)
            Text(
                text = "UPGRADE TO VIP STAGE PASS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure your collection and sync across multiple devices instantly with Google.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glowing Google Sign-In Button
            Button(
                onClick = onGoogleSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .border(1.5.dp, Brush.horizontalGradient(listOf(NeonPink, NeonCyan)), RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
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
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            // Visual Barcode Mockup
            MockBarcode()
        }
    }
}

@Composable
fun VIPTicketStub(
    email: String,
    displayName: String,
    photoUrl: String?,
    onSignOutClick: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Ticket Body
            Text(
                text = "VIP STAGE PASS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // User Photo or Icon
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, NeonCyan, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(2.dp, NeonCyan, CircleShape),
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dashed Divider
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
                    strokeWidth = 2f
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tear-Off Stub (Sync details & Logout)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLOUD SYNC ACTIVE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonYellow,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "All your tickets, wishes, and reviews are synced across your devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Sign-Out Button
            Button(
                onClick = onSignOutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(45.dp)
                    .border(1.dp, NeonPink, RoundedCornerShape(22.5.dp))
                    .clip(RoundedCornerShape(22.5.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        tint = NeonPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIGN OUT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Visual Barcode Mockup
            MockBarcode()
        }
    }
}

@Composable
fun MockBarcode() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(50.dp)
    ) {
        val width = size.width
        val lineCount = 35
        var currentX = 0f
        val spaceRemaining = width
        val segmentWidth = spaceRemaining / lineCount

        // Standard seed values to create a realistic look
        val heights = listOf(
            2f, 4f, 1f, 3f, 5f, 2f, 1f, 4f, 2f, 3f, 1f, 5f, 2f, 4f, 3f, 1f, 2f, 4f, 1f, 3f, 5f, 2f, 1f, 4f, 2f, 3f, 1f, 5f, 2f, 4f, 3f, 1f, 2f, 4f, 2f
        )

        for (i in 0 until lineCount) {
            val lineWidth = heights[i % heights.size]
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(currentX, 0f),
                end = Offset(currentX, size.height),
                strokeWidth = lineWidth
            )
            currentX += segmentWidth
        }
    }
}
