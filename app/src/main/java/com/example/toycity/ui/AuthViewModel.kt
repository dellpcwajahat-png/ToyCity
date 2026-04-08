package com.example.toycity.ui

import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import android.util.Log

import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

    private val _displayName = MutableStateFlow(auth.currentUser?.displayName ?: auth.currentUser?.email ?: "")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    user.reload().await()
                    
                    // Update the local display name state instantly
                    _displayName.value = user.displayName ?: newName
                    // Also refresh the user object
                    _user.value = auth.currentUser
                }
            } catch (e: Exception) {
                Log.e("AuthDebug", "Update Profile Error: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    private val _logoutSuccess = MutableStateFlow(false)
    val logoutSuccess: StateFlow<Boolean> = _logoutSuccess.asStateFlow()

    fun resetLogoutSuccess() {
        _logoutSuccess.value = false
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        _logoutSuccess.value = false
        viewModelScope.launch {
            _loading.value = true
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                Log.d("AuthDebug", "Credential received: ${result.credential.type}")
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("AuthDebug", "Credential Error: ${e.message}")
                _loading.value = false
            } catch (e: Exception) {
                Log.e("AuthDebug", "General Error: ${e.message}")
                e.printStackTrace()
                _loading.value = false
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        try {
            when {
                credential is GoogleIdTokenCredential -> {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    _user.value = authResult.user
                    _displayName.value = authResult.user?.displayName ?: authResult.user?.email ?: ""
                }
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    _user.value = authResult.user
                    _displayName.value = authResult.user?.displayName ?: authResult.user?.email ?: ""
                }
                else -> {
                    Log.e("AuthDebug", "Unexpected credential type: ${credential.type}")
                }
            }
        } catch (e: Exception) {
            Log.e("AuthDebug", "Firebase Auth Error: ${e.message}")
            throw e
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        _user.value = null
        _logoutSuccess.value = true
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthDebug", "Logout error: ${e.message}")
            }
        }
    }
}
