package com.freezingapps.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.freezingapps.app.databinding.FragmentFrozenAppsBinding
import com.freezingapps.app.ui.adapter.FrozenAppAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel

/**
 * Fragment displaying only currently frozen apps.
 * Default/first tab in the main view.
 * Shows a clean, minimalist list with quick unfreeze toggles.
 */
class FrozenAppsFragment : Fragment() {

    private var _binding: FragmentFrozenAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var frozenAppAdapter: FrozenAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFrozenAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        frozenAppAdapter = FrozenAppAdapter(
            onUnfreeze = { appInfo ->
                viewModel.unfreezeApp(appInfo)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = frozenAppAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApps()
        }
    }

    private fun observeViewModel() {
        viewModel.frozenApps.observe(viewLifecycleOwner) { frozenApps ->
            frozenAppAdapter.submitList(frozenApps)

            if (frozenApps.isEmpty()) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
