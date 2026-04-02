package com.freezingapps.app.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freezingapps.app.R
import com.freezingapps.app.data.model.AppInfo

/** Semi-transparent blue overlay for frozen apps (same as Frozen tab). */
private val FrozenOverlay = Color(0x401565C0)

/** Distinct overlay for apps selected to move to Frozen tab. */
private val SelectedOverlay = Color(0x4000897B)

/** Transparent overlay for default state. */
private val DefaultOverlay = Color.Transparent

/** Dark surface color used as card background. */
private val DarkSurface = Color(0xFF1E1E1E)

/**
 * Composable that displays all installed apps in a minimalistic grid layout.
 *
 * Uses color-coded overlays to indicate state:
 * - Already frozen → semi-transparent blue overlay (same as Frozen tab)
 * - Selected for moving to Frozen tab → distinct teal/green overlay
 * - Default → no overlay
 *
 * Tapping an app toggles its selection for moving to the Frozen tab.
 * No checkboxes or tick marks are used.
 *
 * @param apps List of all installed apps to display
 * @param onAppClick Called when user taps an app icon (toggle selection)
 * @param modifier Modifier for the grid container
 */
@Composable
fun AllAppsGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = apps,
            key = { it.packageName }
        ) { appInfo ->
            AllAppsGridItem(
                appInfo = appInfo,
                onAppClick = onAppClick
            )
        }
    }
}

/**
 * Individual grid cell for an app in the All Apps tab.
 *
 * Overlay logic:
 * - Frozen apps → blue overlay (consistent with Frozen tab)
 * - Selected apps (not frozen) → teal overlay (distinct selection color)
 * - Default → no overlay
 *
 * @param appInfo The app data to display
 * @param onAppClick Called when the app is tapped (toggle selection)
 */
@Composable
fun AllAppsGridItem(
    appInfo: AppInfo,
    onAppClick: (AppInfo) -> Unit
) {
    val overlayColor by animateColorAsState(
        targetValue = when {
            appInfo.isInFrozenList -> FrozenOverlay
            appInfo.isSelected -> SelectedOverlay
            else -> DefaultOverlay
        },
        animationSpec = tween(durationMillis = 300),
        label = "allAppsOverlay"
    )

    val nameColor = when {
        appInfo.isInFrozenList -> Color(0xFFB0BEC5)
        appInfo.isSelected -> Color(0xFFA5D6A7)
        else -> Color(0xFFE0E0E0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppClick(appInfo) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            // App icon with color overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                AppIconImage(
                    icon = appInfo.icon,
                    contentDescription = appInfo.appName,
                    modifier = Modifier.size(56.dp)
                )
                // Overlay indicating frozen or selected state
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(overlayColor)
                )
            }

            // App name
            Text(
                text = appInfo.appName,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = nameColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}

/**
 * Empty state composable shown when no apps match the current filter.
 */
@Composable
fun AllAppsEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_empty),
                contentDescription = "No apps found",
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF757575)
            )
            Text(
                text = "No apps found",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
                color = Color(0xFFBDBDBD)
            )
        }
    }
}
