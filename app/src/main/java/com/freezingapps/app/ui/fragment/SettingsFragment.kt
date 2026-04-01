package com.freezingapps.app.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.freezingapps.app.R
import com.freezingapps.app.databinding.FragmentSettingsBinding
import com.freezingapps.app.ui.activity.HistoryActivity
import com.freezingapps.app.ui.activity.ScheduleActivity
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment for app settings and bulk operations.
 * Provides dark mode toggle, bulk freeze/unfreeze,
 * backup, schedule, and advanced options.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

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
            Snackbar.make(binding.root, R.string.backup_complete, Snackbar.LENGTH_SHORT).show()
        }

        // View action history
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }

        // Biometric auth toggle
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isAuthEnabled = prefs.getBoolean("auth_enabled", false)
        binding.authSwitch.isChecked = isAuthEnabled
        binding.authSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auth_enabled", isChecked).apply()
        }

        // Notifications toggle
        val notifEnabled = prefs.getBoolean("notifications_enabled", true)
        binding.notificationsSwitch.isChecked = notifEnabled
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
