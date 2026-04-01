package com.freezingapps.app.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.freezingapps.app.R
import com.freezingapps.app.databinding.ActivitySettingsBinding

/**
 * Settings activity for app configuration.
 * Provides options for theme, authentication, and other preferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        setupThemeToggle()
        setupAuthToggle()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupThemeToggle() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
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

    private fun setupAuthToggle() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isAuthEnabled = prefs.getBoolean("auth_enabled", false)
        binding.authSwitch.isChecked = isAuthEnabled

        binding.authSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auth_enabled", isChecked).apply()
        }
    }
}
