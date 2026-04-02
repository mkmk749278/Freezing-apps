package com.freezingapps.app.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.freezingapps.app.R
import com.freezingapps.app.databinding.FragmentFrozenAppsBinding
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.security.AppLockManager
import com.freezingapps.app.ui.compose.FrozenAppsEmptyState
import com.freezingapps.app.ui.compose.FrozenAppsGrid
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment displaying the managed frozen apps in a launcher-style grid layout.
 * Default/first tab in the main view.
 *
 * Features:
 * - Displays frozen apps as a grid of icons (like a home screen launcher)
 * - Tapping an app icon unfreezes it and launches it immediately
 * - Each app has a fallback "Unfreeze" button for toggle-only operation
 * - Selection checkboxes for the Freeze All bulk operation
 * - Floating action button to freeze all selected apps
 * - Search bar with live filtering
 * - Biometric/PIN authentication before sensitive actions
 * - Pull-to-refresh for manual reload
 * - Empty state when no apps are in the frozen list
 *
 * The grid layout uses Jetpack Compose's LazyVerticalGrid embedded in a
 * ComposeView within the existing XML layout. This hybrid approach keeps
 * the search bar and FAB as traditional views while the grid content
 * uses Compose for modern, efficient rendering.
 */
class FrozenAppsFragment : Fragment() {

    private var _binding: FragmentFrozenAppsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
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
        setupComposeGrid()
        setupSearchBar()
        setupFreezeAllFab()
        setupSwipeRefresh()
        observeAppLaunch()
    }

    /**
     * Setup the Jetpack Compose grid inside the ComposeView.
     *
     * The ComposeView bridges the traditional XML layout with Compose.
     * Inside it, we observe the ViewModel's LiveData as Compose state using
     * observeAsState(), which triggers recomposition whenever the data changes.
     *
     * Grid behavior:
     * - When the app list is empty, shows an empty state message.
     * - When apps exist, shows the LazyVerticalGrid with app icons.
     * - Tapping an icon calls unfreezeAndLaunchApp() (auth-gated).
     * - The fallback "Unfreeze" button calls toggleFreezeState() (auth-gated).
     * - Checkbox toggles selection for the "Freeze All" FAB operation.
     */
    private fun setupComposeGrid() {
        binding.composeView.setContent {
            MaterialTheme {
                // Observe LiveData as Compose state — recomposes on data change.
                // This is the bridge between MVVM LiveData and Compose's reactive model.
                val apps by viewModel.filteredManagedFrozenApps.observeAsState(initial = emptyList())

                if (apps.isEmpty()) {
                    // Show empty state when no frozen apps are managed
                    FrozenAppsEmptyState()
                } else {
                    // Display the launcher-style grid of frozen app icons.
                    // Each icon tap triggers unfreeze + launch (authenticated).
                    FrozenAppsGrid(
                        apps = apps,
                        onAppClick = { appInfo ->
                            // Tap on app icon: authenticate, then unfreeze and launch.
                            // This provides the primary "tap to use" experience.
                            performAuthenticatedAction {
                                viewModel.unfreezeAndLaunchApp(appInfo)
                            }
                        },
                        onToggleFreeze = { appInfo ->
                            // Fallback button: authenticate, then toggle freeze state.
                            // Does NOT launch the app — only changes frozen/active status.
                            performAuthenticatedAction {
                                viewModel.toggleFreezeState(appInfo)
                            }
                        },
                        onSelectionChanged = { appInfo ->
                            // Checkbox toggle for Freeze All selection.
                            // No authentication needed for selection changes.
                            viewModel.toggleManagedFrozenSelection(appInfo)
                        }
                    )
                }
            }
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
     * Shows a confirmation dialog before freezing all selected apps.
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
     * Only freezes apps that are currently selected (checked) in the Frozen tab.
     */
    private fun showFreezeAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.freeze_all)
            .setMessage(R.string.freeze_all_selected_confirm)
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

        // Observe loading state to control swipe refresh indicator
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
    }

    /**
     * Observe the appToLaunch signal from the ViewModel.
     *
     * When unfreezeAndLaunchApp() completes successfully, the ViewModel posts
     * the package name to appToLaunch LiveData. This observer picks it up and
     * creates a launch intent using PackageManager.getLaunchIntentForPackage().
     *
     * Launch flow:
     * 1. User taps app icon in the grid
     * 2. Fragment calls viewModel.unfreezeAndLaunchApp() (after auth)
     * 3. ViewModel unfreezes the app via root command
     * 4. ViewModel posts package name to appToLaunch LiveData
     * 5. This observer receives it and starts the app's launch activity
     */
    private fun observeAppLaunch() {
        viewModel.appToLaunch.observe(viewLifecycleOwner) { packageName ->
            if (packageName.isNullOrBlank()) return@observe
            // Clear the signal immediately to prevent re-launch on rotation
            viewModel.clearAppToLaunch()
            launchApp(packageName)
        }
    }

    /**
     * Launch an app by its package name using the system PackageManager.
     * Creates a launch intent for the app's main activity and starts it.
     * Shows an error message if the app has no launchable activity.
     *
     * @param packageName The package name of the app to launch
     */
    private fun launchApp(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Snackbar.make(
                binding.root,
                "Cannot launch: no activity found for this app",
                Snackbar.LENGTH_SHORT
            ).show()
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
