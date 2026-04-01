package com.freezingapps.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * Minimalist RecyclerView adapter for the Frozen Apps tab.
 * Displays only frozen apps with a quick unfreeze button.
 *
 * @property onUnfreeze Callback when user taps the unfreeze button
 */
class FrozenAppAdapter(
    private val onUnfreeze: (AppInfo) -> Unit
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
        private val iconView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameView: TextView = itemView.findViewById(R.id.appName)
        private val packageView: TextView = itemView.findViewById(R.id.appPackage)
        private val unfreezeButton: MaterialButton = itemView.findViewById(R.id.unfreezeButton)

        fun bind(appInfo: AppInfo) {
            if (appInfo.icon != null) {
                iconView.setImageDrawable(appInfo.icon)
            } else {
                iconView.setImageResource(R.drawable.ic_app_default)
            }

            nameView.text = appInfo.appName
            packageView.text = appInfo.packageName
            cardView.alpha = 0.85f

            unfreezeButton.setOnClickListener {
                onUnfreeze(appInfo)
            }

            cardView.setOnClickListener {
                onUnfreeze(appInfo)
            }
        }
    }

    class FrozenAppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.isFrozen == newItem.isFrozen &&
                    oldItem.appName == newItem.appName
        }
    }
}
