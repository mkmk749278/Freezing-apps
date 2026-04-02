package com.freezingapps.app.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.freezingapps.app.databinding.FragmentAllAppsBinding
import com.freezingapps.app.ui.compose.AllAppsEmptyState
import com.freezingapps.app.ui.compose.AllAppsGrid
import com.freezingapps.app.ui.viewmodel.AppViewModel

/**
 * Fragment displaying all installed apps in a minimalistic grid with color-coded overlays.
 *
 * Uses color overlays instead of checkboxes to indicate state:
 * - Already frozen apps → blue overlay (consistent with Frozen tab)
 * - Selected apps → teal overlay (distinct selection indicator)
 * - Tap an app icon → toggle selection for moving to Frozen tab
 * - FAB "Move to Frozen" → moves all selected apps to the Frozen tab
 */
class AllAppsFragment : Fragment() {

    private var _binding: FragmentAllAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupComposeGrid()
        setupSearch()
        setupFilterChips()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }

    /**
     * Setup the Jetpack Compose grid inside the ComposeView.
     * Displays all installed apps with color-coded overlays for frozen and selected states.
     * Tapping an app toggles its selection for moving to the Frozen tab.
     * Long-pressing a frozen app temporarily unfreezes it.
     */
    private fun setupComposeGrid() {
        binding.composeView.setContent {
            val apps by viewModel.filteredApps.observeAsState(initial = emptyList())

            if (apps.isEmpty()) {
                AllAppsEmptyState()
            } else {
                AllAppsGrid(
                    apps = apps,
                    onAppClick = { appInfo ->
                        viewModel.toggleSelection(appInfo)
                    },
                    onAppLongClick = { appInfo ->
                        if (appInfo.isFrozen) {
                            viewModel.unfreezeApp(appInfo)
                        }
                    }
                )
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        binding.chipSystemApps.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSystemApps(isChecked)
        }

        binding.chipFrozenOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowOnlyFrozen(isChecked)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApps()
        }
    }

    /**
     * Setup the FAB for moving selected apps to the Frozen tab.
     * Always visible — tapping it moves all selected apps to the Frozen list.
     */
    private fun setupFab() {
        binding.fabMoveToFrozen.setOnClickListener {
            viewModel.addSelectedToFrozenList()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
