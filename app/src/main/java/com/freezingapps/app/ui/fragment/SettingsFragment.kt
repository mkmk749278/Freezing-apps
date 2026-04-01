package com.freezingapps.app.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.freezingapps.app.R
import com.freezingapps.app.databinding.FragmentSettingsBinding
import com.freezingapps.app.security.AppLockManager
import com.freezingapps.app.ui.activity.HistoryActivity
import com.freezingapps.app.ui.activity.ScheduleActivity
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment for app settings and bulk operations.
 * Provides dark mode toggle, bulk freeze/unfreeze,
 * backup, schedule, and advanced options.
 *
 * Integrates AppLockManager for PIN setup/removal when
 * toggling the biometric/PIN authentication switch.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var appLockManager: AppLockManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appLockManager = AppLockManager(requireContext())
        setupBulkOperations()
        setupSchedule()
        setupDarkModeToggle()
        setupAdvancedOptions()
    }

    private fun setupBulkOperations() {
        binding.btnFreezeAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.freeze_all_apps)
                .setMessage(R.string.freeze_all_confirm)
                .setPositiveButton(R.string.freeze) { _, _ ->
                    viewModel.freezeAllApps()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.btnUnfreezeAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.unfreeze_all_apps)
                .setMessage(R.string.unfreeze_all_confirm)
                .setPositiveButton(R.string.unfreeze) { _, _ ->
                    viewModel.unfreezeAllApps()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun setupSchedule() {
        binding.btnSchedule.setOnClickListener {
            startActivity(Intent(requireContext(), ScheduleActivity::class.java))
        }
    }

    private fun setupDarkModeToggle() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        binding.darkModeSwitch.isChecked = isDarkMode

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun setupAdvancedOptions() {
        // Backup freeze states
        binding.btnBackup.setOnClickListener {
            viewModel.backupFreezeStates()
        }

        // View action history
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }

        // Biometric/PIN auth toggle — integrated with AppLockManager
        binding.authSwitch.isChecked = appLockManager.isAuthEnabled()
        binding.authSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Require PIN setup before enabling auth
                showPinSetupDialog()
            } else {
                // Disable auth and clear PIN
                appLockManager.setAuthEnabled(false)
                showSnackbar(getString(R.string.auth_disabled))
            }
        }

        // Notifications toggle
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val notifEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.notificationsSwitch.isChecked = notifEnabled
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
        }
    }

    /**
     * Show PIN setup dialog when user enables authentication.
     * Requires entering and confirming a 4-8 digit PIN.
     * If user cancels, the auth switch reverts to off.
     */
    private fun showPinSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_setup, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)
        val confirmInput = dialogView.findViewById<EditText>(R.id.confirmPinInput)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pin_setup_title)
            .setView(dialogView)
            .setPositiveButton(R.string.set_pin, null) // Set below to prevent auto-dismiss
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Revert switch if user cancels
                binding.authSwitch.isChecked = false
            }
            .setOnCancelListener {
                binding.authSwitch.isChecked = false
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val pin = pinInput.text?.toString().orEmpty()
                val confirmPin = confirmInput.text?.toString().orEmpty()

                when {
                    pin.length < 4 -> {
                        pinInput.error = getString(R.string.pin_too_short)
                    }
                    pin != confirmPin -> {
                        confirmInput.error = getString(R.string.pin_mismatch)
                    }
                    else -> {
                        if (appLockManager.setPin(pin)) {
                            appLockManager.setAuthEnabled(true)
                            showSnackbar(getString(R.string.auth_enabled))
                            dialog.dismiss()
                        } else {
                            pinInput.error = getString(R.string.pin_invalid)
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showSnackbar(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
