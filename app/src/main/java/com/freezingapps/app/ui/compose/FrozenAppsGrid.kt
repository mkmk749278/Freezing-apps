package com.freezingapps.app.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.freezingapps.app.R
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.ui.compose.GridColors.ActiveOverlay
import com.freezingapps.app.ui.compose.GridColors.ActiveTextColor
import com.freezingapps.app.ui.compose.GridColors.DarkSurface
import com.freezingapps.app.ui.compose.GridColors.EmptyStateColor
import com.freezingapps.app.ui.compose.GridColors.EmptyStateTextColor
import com.freezingapps.app.ui.compose.GridColors.FrozenOverlay
import com.freezingapps.app.ui.compose.GridColors.FrozenTextColor
import com.freezingapps.app.ui.compose.GridColors.MutedIconTint

/**
 * Composable that displays frozen apps in a minimalistic, dark-themed grid layout
 * with color-coded overlays indicating frozen state.
 *
 * No checkboxes or tick marks are used. Frozen apps show a semi-transparent blue
 * overlay that smoothly animates away when the app is temporarily unfrozen.
 *
 * @param apps List of frozen apps to display in the grid
 * @param onAppClick Called when user taps an app icon (unfreeze + launch)
 * @param modifier Modifier for the grid container
 */
@Composable
fun FrozenAppsGrid(
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
            FrozenAppGridItem(
                appInfo = appInfo,
                onAppClick = onAppClick
            )
        }
    }
}

/**
 * Individual grid cell for a frozen app with color-coded overlay.
 *
 * Frozen apps display a semi-transparent blue overlay on the icon that
 * smoothly animates to transparent when temporarily unfrozen. A slight
 * elevation provides depth. No checkboxes or buttons are shown.
 *
 * @param appInfo The frozen app data to display
 * @param onAppClick Called when the app icon is tapped (unfreeze + launch)
 */
@Composable
fun FrozenAppGridItem(
    appInfo: AppInfo,
    onAppClick: (AppInfo) -> Unit
) {
    val overlayColor by animateColorAsState(
        targetValue = if (appInfo.isFrozen) FrozenOverlay else ActiveOverlay,
        animationSpec = tween(durationMillis = 300),
        label = "frozenOverlay"
    )

    val nameColor = if (appInfo.isFrozen) FrozenTextColor else ActiveTextColor

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
                // Semi-transparent overlay indicating frozen state
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
 * Renders an app icon from an Android Drawable.
 * Converts the Drawable to a Bitmap for Compose Image rendering.
 * Falls back to a default icon if the drawable is null.
 */
@Composable
fun AppIconImage(
    icon: Drawable?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (icon != null) {
        val bitmap = remember(icon) { icon.toBitmap(128, 128).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_app_default),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = MutedIconTint
        )
    }
}

/**
 * Empty state composable shown when no frozen apps are in the list.
 */
@Composable
fun FrozenAppsEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_empty),
                contentDescription = "No frozen apps",
                modifier = Modifier.size(80.dp),
                tint = EmptyStateColor
            )
            Text(
                text = "No apps in Frozen list",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
                color = EmptyStateTextColor
            )
            Text(
                text = "Add apps from the All Apps tab to manage them here",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                color = EmptyStateColor
            )
        }
    }
}
