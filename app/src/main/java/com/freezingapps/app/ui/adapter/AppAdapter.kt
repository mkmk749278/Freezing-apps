package com.freezingapps.app.ui.adapter

import android.annotation.SuppressLint
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
 * RecyclerView adapter for displaying installed apps in the All Apps tab.
 * Supports:
 * - "Add to Frozen" / "Remove" button for managing the frozen list
 * - Multi-select mode for batch adding to frozen list
 *
 * @property onToggleFrozenList Callback for add/remove from frozen list
 * @property onLongClick Callback for entering multi-select mode
 * @property onSelectionChanged Callback when selection state changes
 * @property isMultiSelectMode Whether multi-select mode is active
 */
class AppAdapter(
    private val onToggleFrozenList: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo) -> Unit,
    private val onSelectionChanged: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback()) {

    var isMultiSelectMode: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val iconView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameView: TextView = itemView.findViewById(R.id.appName)
        private val packageView: TextView = itemView.findViewById(R.id.appPackage)
        private val statusView: TextView = itemView.findViewById(R.id.appStatus)
        private val toggleButton: MaterialButton = itemView.findViewById(R.id.toggleButton)
        private val checkBox: CheckBox = itemView.findViewById(R.id.selectionCheckBox)
        private val systemBadge: TextView = itemView.findViewById(R.id.systemBadge)

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
                cardView.alpha = 0.7f
            } else {
                statusView.text = itemView.context.getString(R.string.status_active)
                statusView.setTextColor(itemView.context.getColor(R.color.active_color))
                cardView.alpha = 1.0f
            }

            // Show system badge
            systemBadge.visibility = if (appInfo.isSystemApp) View.VISIBLE else View.GONE

            // Multi-select mode
            if (isMultiSelectMode) {
                checkBox.visibility = View.VISIBLE
                toggleButton.visibility = View.GONE
                checkBox.isChecked = appInfo.isSelected

                checkBox.setOnClickListener {
                    onSelectionChanged(appInfo)
                }

                cardView.setOnClickListener {
                    onSelectionChanged(appInfo)
                }
            } else {
                checkBox.visibility = View.GONE
                toggleButton.visibility = View.VISIBLE

                // Show "Add to Frozen" or "Added" / "Remove" based on frozen list membership
                if (appInfo.isInFrozenList) {
                    toggleButton.text = itemView.context.getString(R.string.added_to_frozen)
                    toggleButton.setIconResource(R.drawable.ic_success)
                } else {
                    toggleButton.text = itemView.context.getString(R.string.add_to_frozen)
                    toggleButton.setIconResource(R.drawable.ic_freeze)
                }

                toggleButton.setOnClickListener {
                    onToggleFrozenList(appInfo)
                }

                cardView.setOnClickListener {
                    onToggleFrozenList(appInfo)
                }
            }

            // Long click to enter multi-select mode
            cardView.setOnLongClickListener {
                onLongClick(appInfo)
                true
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.isFrozen == newItem.isFrozen &&
                    oldItem.isSelected == newItem.isSelected &&
                    oldItem.isInFrozenList == newItem.isInFrozenList &&
                    oldItem.appName == newItem.appName
        }
    }
}
