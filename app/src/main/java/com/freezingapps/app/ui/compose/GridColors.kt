package com.freezingapps.app.ui.compose

import androidx.compose.ui.graphics.Color

/**
 * Shared color constants for grid overlay states used across Frozen and All Apps tabs.
 * Centralizes overlay colors to ensure consistency and easy maintenance.
 */
object GridColors {
    /** Semi-transparent blue overlay for frozen state. */
    val FrozenOverlay = Color(0x401565C0)

    /** Distinct teal overlay for apps selected to move to Frozen tab. */
    val SelectedOverlay = Color(0x4000897B)

    /** Transparent overlay for default (active/unselected) state. */
    val ActiveOverlay = Color.Transparent

    /** Dark surface color used as card background. */
    val DarkSurface = Color(0xFF1E1E1E)

    /** Muted text color for frozen/inactive apps. */
    val FrozenTextColor = Color(0xFFB0BEC5)

    /** Light text color for active apps. */
    val ActiveTextColor = Color(0xFFE0E0E0)

    /** Accent text color for selected apps. */
    val SelectedTextColor = Color(0xFFA5D6A7)

    /** Muted icon tint for empty states and fallback icons. */
    val MutedIconTint = Color(0xFFB0BEC5)

    /** Subdued color for empty state elements. */
    val EmptyStateColor = Color(0xFF757575)

    /** Text color for empty state body text. */
    val EmptyStateTextColor = Color(0xFFBDBDBD)
}
