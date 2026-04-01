package com.freezingapps.app.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.freezingapps.app.databinding.FragmentAllAppsBinding
import com.freezingapps.app.ui.adapter.AppAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel

/**
 * Fragment displaying all installed apps with freeze/unfreeze toggles.
 * Provides search, filter chips, and pull-to-refresh functionality.
 */
class AllAppsFragment : Fragment() {

    private var _binding: FragmentAllAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var appAdapter: AppAdapter

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
        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            onToggleFreeze = { appInfo ->
                viewModel.toggleFreezeState(appInfo)
            },
            onLongClick = { appInfo ->
                viewModel.enterMultiSelectMode()
                viewModel.toggleSelection(appInfo)
            },
            onSelectionChanged = { appInfo ->
                viewModel.toggleSelection(appInfo)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
            setHasFixedSize(false)
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

    private fun observeViewModel() {
        viewModel.filteredApps.observe(viewLifecycleOwner) { apps ->
            appAdapter.submitList(apps)

            if (apps.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.isMultiSelectMode.observe(viewLifecycleOwner) { isMultiSelect ->
            appAdapter.isMultiSelectMode = isMultiSelect
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
