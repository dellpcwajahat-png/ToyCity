package com.example.toycity.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_APP_PIN = "app_pin"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BUSINESS_NAME = "business_name"
    private const val KEY_BUSINESS_ADDRESS = "business_address"
    private const val KEY_BUSINESS_PHONE = "business_phone"
    private const val KEY_THANK_YOU_NOTE = "thank_you_note"
    private const val KEY_SALES_PERSON = "sales_person"
    private const val KEY_NTN_NO = "ntn_no"
    private const val KEY_SELECTED_PRINTER_ADDRESS = "selected_printer_address"
    private const val KEY_PRINTER_PAGE_SIZE = "printer_page_size"

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isAppLockEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun setPin(context: Context, pin: String) {
        getSharedPrefs(context).edit().putString(KEY_APP_PIN, pin).apply()
    }

    fun getPin(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_APP_PIN, null)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun saveReceiptSettings(
        context: Context,
        name: String,
        address: String,
        phone: String,
        note: String,
        salesPerson: String,
        ntnNo: String
    ) {
        getSharedPrefs(context).edit().apply {
            putString(KEY_BUSINESS_NAME, name)
            putString(KEY_BUSINESS_ADDRESS, address)
            putString(KEY_BUSINESS_PHONE, phone)
            putString(KEY_THANK_YOU_NOTE, note)
            putString(KEY_SALES_PERSON, salesPerson)
            putString(KEY_NTN_NO, ntnNo)
        }.apply()
    }

    fun getReceiptSettings(context: Context): Map<String, String> {
        val prefs = getSharedPrefs(context)
        return mapOf(
            "name" to (prefs.getString(KEY_BUSINESS_NAME, "Toy City") ?: "Toy City"),
            "address" to (prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""),
            "phone" to (prefs.getString(KEY_BUSINESS_PHONE, "") ?: ""),
            "note" to (prefs.getString(KEY_THANK_YOU_NOTE, "Thank you for your business!") ?: "Thank you for your business!"),
            "salesPerson" to (prefs.getString(KEY_SALES_PERSON, "") ?: ""),
            "ntnNo" to (prefs.getString(KEY_NTN_NO, "") ?: ""),
            "printerAddress" to (prefs.getString(KEY_SELECTED_PRINTER_ADDRESS, "") ?: ""),
            "pageSize" to (prefs.getString(KEY_PRINTER_PAGE_SIZE, "80mm") ?: "80mm")
        )
    }

    fun getPrinterSettings(context: Context): Map<String, String> {
        val prefs = getSharedPrefs(context)
        return mapOf(
            "address" to (prefs.getString(KEY_SELECTED_PRINTER_ADDRESS, "") ?: ""),
            "pageSize" to (prefs.getString(KEY_PRINTER_PAGE_SIZE, "80mm") ?: "80mm")
        )
    }

    fun savePrinterSettings(context: Context, address: String, pageSize: String) {
        getSharedPrefs(context).edit().apply {
            putString(KEY_SELECTED_PRINTER_ADDRESS, address)
            putString(KEY_PRINTER_PAGE_SIZE, pageSize)
        }.apply()
    }

    fun canAuthenticateWithBiometrics(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Lock")
            .setSubtitle("Unlock Toy City using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
