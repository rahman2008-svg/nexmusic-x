package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeveloperScreen(
    themeColors: ThemeConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Developer",
                tint = themeColors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "About NexMusic X",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                Text(
                    text = "Creators & Ecosystem Context",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LEAD DEVELOPER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(themeColors.primary, themeColors.accent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AR",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        color = themeColors.background
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Prince AR Abdur Rahman",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Lead Architect & Chief Engineer",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Crafted with passion to combine Spotify's aesthetics, Poweramp's custom audio fx pipelines, and Musicolet's secure completely local file player capacity.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- COMPANY PROFILE CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = themeColors.surface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Company",
                    tint = themeColors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NexVora Lab's Ofc",
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onBackground,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Powering cutting edge client-side applications.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ECOSYSTEM PORTFOLIO ---
        Text(
            text = "Ecosystem Applications",
            fontWeight = FontWeight.Bold,
            color = themeColors.primary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        EcosystemItemRow(
            appName = "NexPlay X",
            description = "High resolution video streaming & rendering pipeline.",
            icon = Icons.Default.PlayArrow,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(8.dp))

        EcosystemItemRow(
            appName = "NexLens Studio X",
            description = "AI image filters, raw camera adjustments, and live shaders.",
            icon = Icons.Default.Add,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(8.dp))

        EcosystemItemRow(
            appName = "Study AI",
            description = "Offline assistant & semantic summarizer for student workspaces.",
            icon = Icons.Default.List,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(8.dp))

        EcosystemItemRow(
            appName = "LifeSphere OS",
            description = "Systematic sensory scheduler and biological dashboard.",
            icon = Icons.Default.Favorite,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(8.dp))

        EcosystemItemRow(
            appName = "Smart Day Planner X",
            description = "Time blocking and high latency schedule compiler.",
            icon = Icons.Default.Settings,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Licensed purely under Private Device Policy",
            color = Color.Gray,
            fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EcosystemItemRow(
    appName: String,
    description: String,
    icon: ImageVector,
    themeColors: ThemeConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(themeColors.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = appName,
                    tint = themeColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground,
                    fontSize = 13.sp
                )
                Text(
                    text = description,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
