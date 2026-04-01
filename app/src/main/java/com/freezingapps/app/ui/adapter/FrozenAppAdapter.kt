package com.freezingapps.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freezingapps.app.R
import com.freezingapps.app.data.model.AppInfo
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * RecyclerView adapter for the Frozen Apps tab (managed frozen list).
 * Displays apps added by the user with:
 * - Selection checkbox (for Freeze All operation)
 * - Status indicator (Frozen / Active)
 * - Toggle button to freeze/unfreeze individually
 *
 * @property onToggleFreeze Callback when user taps the toggle button
 * @property onSelectionChanged Callback when selection state changes
 */
class FrozenAppAdapter(
    private val onToggleFreeze: (AppInfo) -> Unit,
    private val onSelectionChanged: (AppInfo) -> Unit
) : ListAdapter<AppInfo, FrozenAppAdapter.FrozenAppViewHolder>(FrozenAppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrozenAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frozen_app, parent, false)
        return FrozenAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrozenAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FrozenAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val checkBox: CheckBox = itemView.findViewById(R.id.selectionCheckBox)
        private val iconView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameView: TextView = itemView.findViewById(R.id.appName)
        private val packageView: TextView = itemView.findViewById(R.id.appPackage)
        private val statusView: TextView = itemView.findViewById(R.id.appStatus)
        private val toggleButton: MaterialButton = itemView.findViewById(R.id.toggleButton)

        fun bind(appInfo: AppInfo) {
            // Set app icon
            if (appInfo.icon != null) {
                iconView.setImageDrawable(appInfo.icon)
            } else {
                iconView.setImageResource(R.drawable.ic_app_default)
            }

            // Set app name and package
            nameView.text = appInfo.appName
            packageView.text = appInfo.packageName

            // Set freeze status
            if (appInfo.isFrozen) {
                statusView.text = itemView.context.getString(R.string.status_frozen)
                statusView.setTextColor(itemView.context.getColor(R.color.frozen_color))
                toggleButton.text = itemView.context.getString(R.string.unfreeze)
                toggleButton.setIconResource(R.drawable.ic_unfreeze)
                cardView.alpha = 0.85f
            } else {
                statusView.text = itemView.context.getString(R.string.status_active)
                statusView.setTextColor(itemView.context.getColor(R.color.active_color))
                toggleButton.text = itemView.context.getString(R.string.freeze)
                toggleButton.setIconResource(R.drawable.ic_freeze)
                cardView.alpha = 1.0f
            }

            // Selection checkbox
            checkBox.setOnCheckedChangeListener(null) // Prevent recursive triggers
            checkBox.isChecked = appInfo.isSelected
            checkBox.setOnCheckedChangeListener { _, _ ->
                onSelectionChanged(appInfo)
            }

            // Toggle freeze/unfreeze
            toggleButton.setOnClickListener {
                onToggleFreeze(appInfo)
            }

            cardView.setOnClickListener {
                onToggleFreeze(appInfo)
            }
        }
    }

    class FrozenAppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.isFrozen == newItem.isFrozen &&
                    oldItem.isSelected == newItem.isSelected &&
                    oldItem.appName == newItem.appName
        }
    }
}
