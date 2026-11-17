package com.mobidesk.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val badge: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tags: List<String>,
    val icon: ImageVector
)

private val pages = listOf(
    OnboardingPage(
        badge = "// [ SYSTEM INIT: CLOUD WORKSPACE ]",
        title = "Welcome to MobiDesk",
        subtitle = "Cloud Workspace Infrastructure",
        description = "Your persistent, high-performance Windows VM workspace in the cloud. Access curriculum software, developer environments, and files seamlessly from any Android device.",
        tags = listOf("HYPERVISOR V2", "TLS 1.3 ENCRYPTED", "ZERO SETUP"),
        icon = Icons.Outlined.CloudDone
    ),
    OnboardingPage(
        badge = "// [ INPUT PIPELINE: ABSOLUTE COORDS ]",
        title = "Zero-Latency Input",
        subtitle = "Direct Touch & Hardware Accel",
        description = "Engineered for natural touch interaction. Native direct touch translates every tap and drag with 1:1 absolute cursor accuracy, backed by 16:9 PC hardware rendering.",
        tags = listOf("16:9 NATIVE HD", "DIRECT TOUCH", "60 FPS ENGINE"),
        icon = Icons.Outlined.TouchApp
    )
)

/**
 * Applet-Style Onboarding Cards with cyberpunk/terminal aesthetic.
 * Swipeable via horizontal drag or button controls, with instant dismiss to login.
 */
@Composable
fun OnboardingCards(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var totalDrag by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag < -60f && currentPage < pages.size - 1) {
                            currentPage++
                        } else if (totalDrag > 60f && currentPage > 0) {
                            currentPage--
                        } else if (totalDrag < -100f && currentPage == pages.size - 1) {
                            onDismiss()
                        }
                    }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF090D16)
        ),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            val page = pages[currentPage]

            // Top Header row with badge and dismiss close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = page.badge,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFCC),
                    letterSpacing = 0.1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF80A0C0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_content"
            ) { targetPage ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = targetPage.icon,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = targetPage.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                            )
                            Text(
                                text = targetPage.subtitle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = targetPage.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF94A3B8),
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    // Cyber telemetry tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targetPage.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Navigation Controls and Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 20.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentPage) Color(0xFF00E5FF)
                                    else Color(0xFF334155)
                                )
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (currentPage > 0) {
                        TextButton(
                            onClick = { currentPage-- }
                        ) {
                            Text("< Back", color = Color(0xFF94A3B8))
                        }
                    }

                    if (currentPage < pages.size - 1) {
                        Button(
                            onClick = { currentPage++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF0A0E17)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Next >", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FFCC),
                                contentColor = Color(0xFF0A0E17)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Got It →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
