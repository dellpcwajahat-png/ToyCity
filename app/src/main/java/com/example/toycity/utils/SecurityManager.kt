package com.example.toycity.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

object SecurityManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_APP_PIN = "app_pin"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BUSINESS_NAME = "business_name"
    private const val KEY_BUSINESS_ADDRESS = "business_address"
    private const val KEY_BUSINESS_PHONE = "business_phone"
    private const val KEY_THANK_YOU_NOTE = "thank_you_note"
    private const val KEY_SALES_PERSON_PREFIX = "sales_person_"
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
        getSharedPrefs(context).edit {
            putBoolean(KEY_APP_LOCK_ENABLED, enabled)
        }
    }

    fun setPin(context: Context, pin: String) {
        getSharedPrefs(context).edit {
            putString(KEY_APP_PIN, pin)
        }
    }

    fun getPin(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_APP_PIN, null)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getSharedPrefs(context).edit {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
        }
    }

    @Deprecated("Use cloud-based functions instead: saveGlobalReceiptSettings, saveUserReceiptSettings")
    fun saveReceiptSettings(
        context: Context,
        name: String,
        address: String,
        phone: String,
        note: String,
        salesPerson: String,
        ntnNo: String,
        userId: String? = null
    ) {
        getSharedPrefs(context).edit {
            putString(KEY_BUSINESS_NAME, name)
            putString(KEY_BUSINESS_ADDRESS, address)
            putString(KEY_BUSINESS_PHONE, phone)
            putString(KEY_THANK_YOU_NOTE, note)
            if (userId != null) {
                putString(KEY_SALES_PERSON_PREFIX + userId, salesPerson)
            } else {
                // Fallback or admin default
                putString(KEY_SALES_PERSON_PREFIX + "admin", salesPerson)
            }
            putString(KEY_NTN_NO, ntnNo)
        }
    }

    fun getReceiptSettings(context: Context, userId: String? = null): Map<String, String> {
        val prefs = getSharedPrefs(context)
        val salesPersonKey = if (userId != null) KEY_SALES_PERSON_PREFIX + userId else KEY_SALES_PERSON_PREFIX + "admin"
        return mapOf(
            "name" to (prefs.getString(KEY_BUSINESS_NAME, "Toy City") ?: "Toy City"),
            "address" to (prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""),
            "phone" to (prefs.getString(KEY_BUSINESS_PHONE, "") ?: ""),
            "note" to (prefs.getString(KEY_THANK_YOU_NOTE, "Thank you for shopping with us") ?: "Thank you for shopping with us"),
            "salesPerson" to (prefs.getString(salesPersonKey, "") ?: ""),
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
        getSharedPrefs(context).edit {
            putString(KEY_SELECTED_PRINTER_ADDRESS, address)
            putString(KEY_PRINTER_PAGE_SIZE, pageSize)
        }
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

    // Cloud sync functions
    private fun getDb() = FirebaseFirestore.getInstance()

    fun isAdminUser(email: String?): Boolean {
        return email == "wajahatabbasicentral@gmail.com"
    }

    suspend fun saveGlobalReceiptSettings(
        name: String,
        address: String,
        phone: String,
        note: String,
        ntnNo: String
    ) {
        val data = mapOf(
            "name" to name,
            "address" to address,
            "phone" to phone,
            "note" to note,
            "ntnNo" to ntnNo
        )
        try {
            withTimeout(15000L) { // 15 second timeout
                getDb().collection("settings").document("global").set(data).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw Exception("Operation timed out. Please check your internet connection and try again.")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw Exception("Permission denied. Please contact administrator.")
            } else {
                throw Exception("Firestore error: ${e.message}")
            }
        } catch (e: Exception) {
            throw Exception("Failed to save global settings: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun saveUserReceiptSettings(userId: String, salesPerson: String) {
        val data = mapOf("salesPerson" to salesPerson)
        try {
            withTimeout(15000L) { // 15 second timeout
                getDb().collection("settings").document("users").collection("userSettings").document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw Exception("Operation timed out. Please check your internet connection and try again.")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw Exception("Permission denied. Please contact administrator.")
            } else {
                throw Exception("Firestore error: ${e.message}")
            }
        } catch (e: Exception) {
            throw Exception("Failed to save user settings: ${e.message ?: "Unknown error"}")
        }
    }

    suspend fun saveUserPrinterSettings(userId: String, address: String, pageSize: String) {
        val data = mapOf(
            "printerAddress" to address,
            "pageSize" to pageSize
        )
        withTimeout(10000L) { // 10 second timeout
            getDb().collection("settings").document("users").collection("userSettings").document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    suspend fun <T> withFirestoreTimeout(timeoutMillis: Long, block: suspend () -> T): T? {
        return withTimeoutOrNull(timeoutMillis) {
            block()
        }
    }

    fun getReceiptSettingsFlow(context: Context, userId: String?): Flow<Map<String, String>> = flow {
        // Emit local settings first
        val localSettings = getReceiptSettings(context, userId)
        emit(localSettings)

        try {
            // Get global settings
            val globalSnapshot = getDb().collection("settings").document("global").get().await()
            val globalData = globalSnapshot.data ?: emptyMap()

            // Get user settings if userId provided
            val userData = if (userId != null) {
                val userSnapshot = getDb().collection("settings").document("users").collection("userSettings").document(userId).get().await()
                userSnapshot.data ?: emptyMap()
            } else {
                emptyMap()
            }

            // Merge global and user settings
            // Note: salesPerson should default to empty string for new users
            val merged = mapOf(
                "name" to ((globalData["name"] as? String) ?: localSettings["name"] ?: "Toy City"),
                "address" to ((globalData["address"] as? String) ?: localSettings["address"] ?: ""),
                "phone" to ((globalData["phone"] as? String) ?: localSettings["phone"] ?: ""),
                "note" to ((globalData["note"] as? String) ?: localSettings["note"] ?: "Thank you for shopping with us"),
                "ntnNo" to ((globalData["ntnNo"] as? String) ?: localSettings["ntnNo"] ?: ""),
                "salesPerson" to ((userData["salesPerson"] as? String) ?: ""),
                "printerAddress" to ((userData["printerAddress"] as? String) ?: localSettings["printerAddress"] ?: ""),
                "pageSize" to ((userData["pageSize"] as? String) ?: localSettings["pageSize"] ?: "80mm")
            )
            emit(merged)
        } catch (_: Exception) {
            // If cloud fails, keep local
        }
    }

    fun getPrinterSettingsFlow(context: Context, userId: String?): Flow<Map<String, String>> = flow {
        val localSettings = getPrinterSettings(context)
        emit(localSettings)

        try {
            val userDoc = if (userId != null) {
                getDb().collection("settings").document("users").collection("userSettings").document(userId).get().await()
            } else null
            val userData = userDoc?.data ?: emptyMap()

            val merged = mapOf(
                "address" to ((userData["printerAddress"] as? String) ?: localSettings["address"] ?: ""),
                "pageSize" to ((userData["pageSize"] as? String) ?: localSettings["pageSize"] ?: "80mm")
            )
            emit(merged)
        } catch (_: Exception) {
            // Keep local
        }
    }
}
