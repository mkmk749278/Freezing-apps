package com.freezingapps.app.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.freezingapps.app.R
import com.freezingapps.app.databinding.FragmentFrozenAppsBinding
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.security.AppLockManager
import com.freezingapps.app.ui.adapter.FrozenAppAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment displaying only currently frozen apps.
 * Default/first tab in the main view.
 *
 * Features:
 * - Search bar with live filtering by app name or package name
 * - Floating action button to freeze all non-frozen apps
 * - Quick unfreeze button on each frozen app
 * - Biometric/PIN authentication before sensitive actions
 * - Pull-to-refresh for manual reload
 * - Empty state when no frozen apps exist
 */
class FrozenAppsFragment : Fragment() {

    private var _binding: FragmentFrozenAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var frozenAppAdapter: FrozenAppAdapter
    private lateinit var appLockManager: AppLockManager

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
        appLockManager = AppLockManager(requireContext())
        setupRecyclerView()
        setupSearchBar()
        setupFreezeAllFab()
        setupSwipeRefresh()
        observeViewModel()
    }

    /**
     * Initialize RecyclerView with the frozen app adapter.
     * Unfreeze actions require authentication when enabled.
     */
    private fun setupRecyclerView() {
        frozenAppAdapter = FrozenAppAdapter(
            onUnfreeze = { appInfo ->
                performAuthenticatedAction {
                    viewModel.unfreezeApp(appInfo)
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = frozenAppAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * Setup the search bar with live filtering.
     * Filters frozen apps by name or package name as user types.
     */
    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchFrozenApps(s?.toString().orEmpty())
            }
        })
    }

    /**
     * Setup the Freeze All floating action button.
     * Shows a confirmation dialog before freezing all non-frozen apps.
     * Requires authentication when app lock is enabled.
     */
    private fun setupFreezeAllFab() {
        binding.fabFreezeAll.setOnClickListener {
            performAuthenticatedAction {
                showFreezeAllConfirmation()
            }
        }
    }

    /**
     * Show confirmation dialog before executing Freeze All operation.
     */
    private fun showFreezeAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.freeze_all)
            .setMessage(R.string.freeze_all_confirm)
            .setPositiveButton(R.string.freeze) { _, _ ->
                viewModel.freezeAllInFrozenTab()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApps()
        }
    }

    /**
     * Observe ViewModel LiveData for frozen apps and loading state.
     * Uses filteredFrozenApps to support search filtering.
     */
    private fun observeViewModel() {
        viewModel.filteredFrozenApps.observe(viewLifecycleOwner) { frozenApps ->
            frozenAppAdapter.submitList(frozenApps)
            updateEmptyState(frozenApps)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    /**
     * Update empty state visibility based on frozen apps list.
     */
    private fun updateEmptyState(frozenApps: List<AppInfo>) {
        if (frozenApps.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Execute an action after authentication (if enabled).
     * Tries biometric first, falls back to PIN dialog.
     * If auth is not enabled, executes the action immediately.
     *
     * @param action The action to execute after successful authentication
     */
    private fun performAuthenticatedAction(action: () -> Unit) {
        if (!appLockManager.isAuthEnabled() || !appLockManager.isPinSet()) {
            action()
            return
        }

        val activity = requireActivity()
        appLockManager.showBiometricPrompt(
            activity = activity as androidx.fragment.app.FragmentActivity,
            onSuccess = { action() },
            onFailure = {
                // Biometric failed or unavailable — show PIN dialog
                showPinVerifyDialog(action)
            }
        )
    }

    /**
     * Show PIN verification dialog for authentication fallback.
     * Validates entered PIN against stored hash.
     */
    private fun showPinVerifyDialog(onSuccess: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_verify, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)
        val errorText = dialogView.findViewById<TextView>(R.id.errorText)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pin_verify_title)
            .setView(dialogView)
            .setPositiveButton(R.string.verify, null) // Set below to prevent auto-dismiss
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val pin = pinInput.text?.toString().orEmpty()
                if (appLockManager.verifyPin(pin)) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    errorText.text = getString(R.string.incorrect_pin)
                    errorText.visibility = View.VISIBLE
                    pinInput.text?.clear()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
