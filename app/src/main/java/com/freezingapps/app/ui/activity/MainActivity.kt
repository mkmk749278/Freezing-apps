package com.freezingapps.app.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.freezingapps.app.R
import com.freezingapps.app.databinding.ActivityMainBinding
import com.freezingapps.app.ui.adapter.AppAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Main activity displaying the list of installed apps.
 * Provides freeze/unfreeze functionality with search, filter,
 * and multi-select capabilities.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels()
    private lateinit var appAdapter: AppAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification permission result - no action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        requestNotificationPermission()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Setup search view
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchApps(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter_system -> {
                item.isChecked = !item.isChecked
                viewModel.setShowSystemApps(item.isChecked)
                true
            }
            R.id.action_filter_frozen -> {
                item.isChecked = !item.isChecked
                viewModel.setShowOnlyFrozen(item.isChecked)
                true
            }
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.action_schedule -> {
                startActivity(Intent(this, ScheduleActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_select_all -> {
                if (viewModel.isMultiSelectMode.value == true) {
                    viewModel.selectAll()
                } else {
                    viewModel.enterMultiSelectMode()
                    viewModel.selectAll()
                    updateMultiSelectUI(true)
                }
                true
            }
            R.id.action_refresh -> {
                viewModel.loadApps()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Handle back press to exit multi-select mode first.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use OnBackPressedCallback")
    override fun onBackPressed() {
        if (viewModel.isMultiSelectMode.value == true) {
            viewModel.exitMultiSelectMode()
            updateMultiSelectUI(false)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Request notification permission on Android 13+.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Setup the RecyclerView with the app adapter.
     */
    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            onToggleFreeze = { appInfo ->
                viewModel.toggleFreezeState(appInfo)
            },
            onLongClick = { appInfo ->
                viewModel.enterMultiSelectMode()
                viewModel.toggleSelection(appInfo)
                updateMultiSelectUI(true)
            },
            onSelectionChanged = { appInfo ->
                viewModel.toggleSelection(appInfo)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * Setup swipe-to-refresh for reloading the app list.
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApps()
        }
    }

    /**
     * Setup the Floating Action Button for bulk operations.
     */
    private fun setupFab() {
        binding.fabBulkAction.setOnClickListener {
            if (viewModel.isMultiSelectMode.value == true) {
                showBulkActionDialog()
            } else {
                viewModel.enterMultiSelectMode()
                updateMultiSelectUI(true)
                Snackbar.make(binding.root, R.string.multi_select_hint, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Show dialog for bulk freeze/unfreeze operations.
     */
    private fun showBulkActionDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.bulk_action_title)
            .setItems(arrayOf(
                getString(R.string.freeze_selected),
                getString(R.string.unfreeze_selected),
                getString(R.string.cancel_selection)
            )) { _, which ->
                when (which) {
                    0 -> viewModel.freezeSelected()
                    1 -> viewModel.unfreezeSelected()
                    2 -> {
                        viewModel.exitMultiSelectMode()
                        updateMultiSelectUI(false)
                    }
                }
            }
            .show()
    }

    /**
     * Observe ViewModel LiveData and update UI accordingly.
     */
    private fun observeViewModel() {
        // Observe root availability
        viewModel.isRootAvailable.observe(this) { isAvailable ->
            if (!isAvailable) {
                showNoRootDialog()
            }
        }

        // Observe filtered apps list
        viewModel.filteredApps.observe(this) { apps ->
            appAdapter.submitList(apps)

            // Show empty state
            if (apps.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe messages
        viewModel.message.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }

        // Observe multi-select mode
        viewModel.isMultiSelectMode.observe(this) { isMultiSelect ->
            appAdapter.isMultiSelectMode = isMultiSelect
            updateMultiSelectUI(isMultiSelect)
        }
    }

    /**
     * Update UI elements based on multi-select mode state.
     */
    private fun updateMultiSelectUI(isMultiSelect: Boolean) {
        if (isMultiSelect) {
            binding.fabBulkAction.setImageResource(R.drawable.ic_action)
            supportActionBar?.title = getString(R.string.select_apps)
        } else {
            binding.fabBulkAction.setImageResource(R.drawable.ic_multi_select)
            supportActionBar?.title = getString(R.string.app_name)
        }
    }

    /**
     * Show dialog when root access is not available.
     */
    private fun showNoRootDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.root_required_title)
            .setMessage(R.string.root_required_message)
            .setPositiveButton(R.string.retry) { _, _ ->
                viewModel.checkRootAccess()
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
