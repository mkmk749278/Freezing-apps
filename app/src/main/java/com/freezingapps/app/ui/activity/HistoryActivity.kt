package com.freezingapps.app.ui.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.freezingapps.app.R
import com.freezingapps.app.databinding.ActivityHistoryBinding
import com.freezingapps.app.ui.adapter.HistoryAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Activity for displaying the action history log.
 * Shows all freeze/unfreeze operations with timestamps and status.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: AppViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.action_history)

        setupRecyclerView()
        setupClearButton()
        observeViewModel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupClearButton() {
        binding.clearHistoryButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setPositiveButton(R.string.clear) { _, _ ->
                    viewModel.clearActionLogs()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.actionLogs.observe(this) { logs ->
            historyAdapter.submitList(logs)

            if (logs.isEmpty()) {
                binding.emptyHistoryView.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
                binding.clearHistoryButton.visibility = View.GONE
            } else {
                binding.emptyHistoryView.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                binding.clearHistoryButton.visibility = View.VISIBLE
            }
        }
    }
}
