package com.freezingapps.app.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.freezingapps.app.R
import com.freezingapps.app.data.model.AppInfo

/**
 * Composable that displays frozen apps in a launcher-style grid layout.
 *
 * Grid Layout Design:
 * - Uses LazyVerticalGrid with GridCells.Adaptive(minSize = 88.dp) so the grid
 *   automatically calculates the number of columns based on the available screen width.
 *   This ensures responsive layout on phones, tablets, and different orientations.
 * - Each grid cell displays the app icon (56dp), app name (single line, truncated),
 *   a small "Unfreeze" fallback button, and a selection checkbox.
 *
 * Click-to-Launch Logic:
 * - Tapping any app icon triggers [onAppClick], which the Fragment wires to the
 *   ViewModel's unfreezeAndLaunchApp(). The ViewModel unfreezes the app via root
 *   command (if frozen), then signals the UI to launch it using PackageManager.
 * - The "Unfreeze" button is a fallback that only toggles freeze state without launching.
 *
 * Performance:
 * - LazyVerticalGrid only composes visible items, supporting large frozen app lists
 *   without memory issues (equivalent to RecyclerView's view recycling).
 * - DiffUtil-like behavior is achieved by using the app's packageName as a stable key
 *   in the items() call, so Compose only recomposes changed items.
 *
 * @param apps List of frozen apps to display in the grid
 * @param onAppClick Called when user taps an app icon (unfreeze + launch)
 * @param onToggleFreeze Called when user taps the "Unfreeze" button (fallback toggle)
 * @param onSelectionChanged Called when user toggles the selection checkbox
 * @param modifier Modifier for the grid container
 */
@Composable
fun FrozenAppsGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onToggleFreeze: (AppInfo) -> Unit,
    onSelectionChanged: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // LazyVerticalGrid provides efficient, scrollable grid layout.
    // GridCells.Adaptive(88.dp) dynamically determines columns based on screen width,
    // ensuring a responsive layout that works on any screen size.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Use packageName as a stable key so Compose can efficiently diff and
        // only recompose items whose data has actually changed.
        items(
            items = apps,
            key = { it.packageName }
        ) { appInfo ->
            FrozenAppGridItem(
                appInfo = appInfo,
                onAppClick = onAppClick,
                onToggleFreeze = onToggleFreeze,
                onSelectionChanged = onSelectionChanged
            )
        }
    }
}

/**
 * Individual grid cell for a frozen app, styled like a launcher icon.
 *
 * Layout (top to bottom):
 * 1. Selection checkbox (top-end corner, for Freeze All selection)
 * 2. App icon (56dp, tappable — triggers unfreeze + launch)
 * 3. App name (single line, centered, ellipsis overflow)
 * 4. Unfreeze/Freeze button (small outlined button as fallback)
 *
 * Frozen apps appear with reduced opacity (0.7f) to visually indicate
 * their disabled state, similar to how Android launchers grey out disabled apps.
 *
 * @param appInfo The frozen app data to display
 * @param onAppClick Called when the app icon is tapped (unfreeze + launch)
 * @param onToggleFreeze Called when the fallback button is tapped
 * @param onSelectionChanged Called when the checkbox is toggled
 */
@Composable
fun FrozenAppGridItem(
    appInfo: AppInfo,
    onAppClick: (AppInfo) -> Unit,
    onToggleFreeze: (AppInfo) -> Unit,
    onSelectionChanged: (AppInfo) -> Unit
) {
    // Reduce opacity for frozen apps to visually indicate disabled state
    val itemAlpha = if (appInfo.isFrozen) 0.7f else 1.0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // App icon — tapping it unfreezes the app and launches it immediately.
            // This is the primary interaction: tap icon → unfreeze → launch.
            AppIconImage(
                icon = appInfo.icon,
                contentDescription = appInfo.appName,
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onAppClick(appInfo) }
            )

            // App name displayed below the icon, single line with ellipsis
            Text(
                text = appInfo.appName,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 2.dp, end = 2.dp)
            )

            // Fallback "Unfreeze" / "Freeze" button — toggles freeze state
            // without launching the app. Kept as a secondary action.
            OutlinedButton(
                onClick = { onToggleFreeze(appInfo) },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = if (appInfo.isFrozen) "Unfreeze" else "Freeze",
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        // Selection checkbox positioned at the top-end corner.
        // Used for selecting apps for the "Freeze All" bulk operation.
        Checkbox(
            checked = appInfo.isSelected,
            onCheckedChange = { onSelectionChanged(appInfo) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        )
    }
}

/**
 * Renders an app icon from an Android Drawable.
 * Converts the Drawable to a Bitmap for Compose Image rendering.
 * Falls back to a default icon if the drawable is null.
 *
 * @param icon The app's launcher icon drawable (may be null for uninstalled apps)
 * @param contentDescription Accessibility description (app name)
 * @param modifier Modifier for sizing and click handling
 */
@Composable
fun AppIconImage(
    icon: Drawable?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (icon != null) {
        // Convert Android Drawable to Compose-compatible ImageBitmap.
        // toBitmap() creates a software bitmap; asImageBitmap() wraps it for Compose.
        // remember(icon) caches the bitmap to avoid recreation on every recomposition.
        val bitmap = remember(icon) { icon.toBitmap(128, 128).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        // Fallback: use the default app icon resource when no icon is available
        Icon(
            painter = painterResource(id = R.drawable.ic_app_default),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

/**
 * Empty state composable shown when no frozen apps are in the list.
 * Displays a message guiding the user to add apps from the All Apps tab.
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No apps in Frozen list",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add apps from the All Apps tab to manage them here",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
