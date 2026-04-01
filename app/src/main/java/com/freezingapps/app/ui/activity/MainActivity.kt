package com.freezingapps.app.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.freezingapps.app.R
import com.freezingapps.app.databinding.ActivityMainBinding
import com.freezingapps.app.security.AppLockManager
import com.freezingapps.app.ui.adapter.MainPagerAdapter
import com.freezingapps.app.ui.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Main activity with three-tab layout using TabLayout + ViewPager2.
 * Tabs: Frozen Apps (default), All Apps, Settings.
 *
 * On launch, requires authentication (fingerprint/PIN) if app lock is enabled.
 * Uses session-based auth: once authenticated, valid for the entire session.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels()
    private lateinit var appLockManager: AppLockManager

    /** Session-based authentication flag. Reset on each app launch. */
    private var isAuthenticated = false

    private val tabTitles by lazy {
        arrayOf(
            getString(R.string.tab_frozen),
            getString(R.string.tab_all_apps),
            getString(R.string.tab_settings)
        )
    }

    private val tabIcons = intArrayOf(
        R.drawable.ic_freeze,
        R.drawable.ic_app_default,
        R.drawable.ic_settings
    )

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification permission result - no action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appLockManager = AppLockManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        requestNotificationPermission()
        setupViewPager()
        observeViewModel()

        // Gate the app behind authentication if enabled
        checkAppLockOnLaunch()
    }

    /**
     * Check if app lock is enabled and require authentication on launch.
     * If auth is enabled and PIN is set, shows biometric prompt or PIN dialog.
     * Content is hidden behind an overlay until authenticated.
     */
    private fun checkAppLockOnLaunch() {
        if (!appLockManager.isAuthEnabled() || !appLockManager.isPinSet()) {
            isAuthenticated = true
            return
        }

        // Disable interaction until authenticated
        binding.viewPager.isUserInputEnabled = false

        appLockManager.showBiometricPrompt(
            activity = this,
            onSuccess = {
                isAuthenticated = true
                binding.viewPager.isUserInputEnabled = true
            },
            onFailure = {
                // Biometric failed or unavailable — show PIN dialog
                showLaunchPinDialog()
            }
        )
    }

    /**
     * Show PIN verification dialog at app launch.
     * User must enter correct PIN to proceed. Cancelling exits the app.
     */
    private fun showLaunchPinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_verify, null)
        val pinInput = dialogView.findViewById<EditText>(R.id.pinInput)
        val errorText = dialogView.findViewById<TextView>(R.id.errorText)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pin_verify_title)
            .setView(dialogView)
            .setPositiveButton(R.string.verify, null)
            .setNegativeButton(R.string.exit) { _, _ -> finish() }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val pin = pinInput.text?.toString().orEmpty()
                if (appLockManager.verifyPin(pin)) {
                    isAuthenticated = true
                    binding.viewPager.isUserInputEnabled = true
                    dialog.dismiss()
                } else {
                    errorText.text = getString(R.string.incorrect_pin)
                    errorText.visibility = View.VISIBLE
                    pinInput.text?.clear()
                }
            }
        }

        dialog.show()
    }

    /**
     * Setup ViewPager2 with TabLayout for the three-tab design.
     * Frozen Apps tab is the default active tab (position 0).
     */
    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Enable swipe for tab navigation
        binding.viewPager.isUserInputEnabled = true

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
            tab.setIcon(tabIcons[position])
        }.attach()

        // Frozen Apps tab is position 0 (default), no need to set explicitly
    }

    /**
     * Observe ViewModel LiveData shared across fragments.
     */
    private fun observeViewModel() {
        viewModel.isRootAvailable.observe(this) { isAvailable ->
            if (!isAvailable) {
                showNoRootDialog()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(this) { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
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
