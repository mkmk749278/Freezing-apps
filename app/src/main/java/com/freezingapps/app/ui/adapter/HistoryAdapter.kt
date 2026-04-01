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
import com.freezingapps.app.data.model.ActionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying action history logs.
 */
class HistoryAdapter : ListAdapter<ActionLog, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val actionIcon: ImageView = itemView.findViewById(R.id.actionIcon)
        private val appNameText: TextView = itemView.findViewById(R.id.historyAppName)
        private val actionText: TextView = itemView.findViewById(R.id.historyAction)
        private val timestampText: TextView = itemView.findViewById(R.id.historyTimestamp)
        private val statusIcon: ImageView = itemView.findViewById(R.id.statusIcon)
        private val errorText: TextView = itemView.findViewById(R.id.historyError)

        fun bind(log: ActionLog) {
            appNameText.text = log.appName
            timestampText.text = dateFormat.format(Date(log.timestamp))

            // Set action text and icon
            if (log.action == "freeze") {
                actionText.text = itemView.context.getString(R.string.action_frozen)
                actionIcon.setImageResource(R.drawable.ic_freeze)
            } else {
                actionText.text = itemView.context.getString(R.string.action_unfrozen)
                actionIcon.setImageResource(R.drawable.ic_unfreeze)
            }

            // Set status icon
            if (log.success) {
                statusIcon.setImageResource(R.drawable.ic_success)
                errorText.visibility = View.GONE
            } else {
                statusIcon.setImageResource(R.drawable.ic_error)
                if (log.errorMessage != null) {
                    errorText.visibility = View.VISIBLE
                    errorText.text = log.errorMessage
                } else {
                    errorText.visibility = View.GONE
                }
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<ActionLog>() {
        override fun areItemsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActionLog, newItem: ActionLog): Boolean {
            return oldItem == newItem
        }
    }
}
