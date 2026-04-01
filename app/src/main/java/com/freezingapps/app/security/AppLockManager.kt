package com.freezingapps.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

/**
 * Manages app-specific lock security using custom PIN and/or biometric authentication.
 *
 * Features:
 * - Custom PIN storage with SHA-256 hashing (not system lock screen PIN)
 * - Biometric (fingerprint) authentication via AndroidX Biometric library
 * - Session-based auth: authenticate once on launch, valid for the session
 * - Graceful fallback: if biometrics unavailable, uses PIN only
 *
 * PIN is stored as a SHA-256 hash in SharedPreferences for security.
 */
class AppLockManager(context: Context) {

    companion object {
        private const val TAG = "AppLockManager"
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_AUTH_ENABLED = "auth_enabled"
        private const val PIN_MIN_LENGTH = 4
        private const val PIN_MAX_LENGTH = 8
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if app lock authentication is enabled.
     */
    fun isAuthEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTH_ENABLED, false)
    }

    /**
     * Enable or disable app lock authentication.
     * When disabling, the stored PIN is cleared for security.
     */
    fun setAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_ENABLED, enabled).apply()
        if (!enabled) {
            clearPin()
        }
        Log.d(TAG, "Auth enabled: $enabled")
    }

    /**
     * Check if a custom PIN has been set.
     */
    fun isPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    /**
     * Set a new custom PIN. Validates length before storing.
     *
     * @param pin The raw PIN string (4-8 digits)
     * @return true if PIN was set successfully, false if validation failed
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) {
            Log.w(TAG, "Invalid PIN: must be $PIN_MIN_LENGTH-$PIN_MAX_LENGTH digits")
            return false
        }
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        Log.d(TAG, "PIN set successfully")
        return true
    }

    /**
     * Verify a PIN against the stored hash.
     *
     * @param pin The raw PIN to verify
     * @return true if PIN matches, false otherwise
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    /**
     * Clear the stored PIN.
     */
    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
        Log.d(TAG, "PIN cleared")
    }

    /**
     * Check if biometric authentication is available on this device.
     *
     * @param context Application or activity context
     * @return true if fingerprint or other biometrics can be used
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Show biometric authentication prompt.
     * Falls back to PIN if biometrics are not available.
     *
     * @param activity The FragmentActivity hosting the biometric prompt
     * @param onSuccess Called when authentication succeeds
     * @param onFailure Called when authentication fails or is cancelled
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isBiometricAvailable(activity)) {
            Log.d(TAG, "Biometrics not available, falling back to PIN")
            onFailure("biometric_unavailable")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "Biometric auth error ($errorCode): $errString")
                // User cancelled or biometric lockout — allow PIN fallback
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed (fingerprint not recognized)")
                // Don't call onFailure here — the system shows "Not recognized" and lets user retry
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Freezing Apps")
            .setSubtitle("Verify your identity")
            .setDescription("Use fingerprint to unlock the app")
            .setNegativeButtonText("Use PIN")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Validate PIN format: must be digits only, 4-8 characters.
     */
    private fun isValidPin(pin: String): Boolean {
        return pin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && pin.all { it.isDigit() }
    }

    /**
     * Hash a PIN using SHA-256 for secure storage.
     * Never store raw PINs.
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
