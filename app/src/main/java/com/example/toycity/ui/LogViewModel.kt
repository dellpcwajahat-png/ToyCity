package com.example.toycity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toycity.data.LogRepository
import com.example.toycity.data.UserLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogViewModel : ViewModel() {
    private val repository = LogRepository()
    private val auth = FirebaseAuth.getInstance()

    val allLogs: StateFlow<List<UserLog>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun logAction(action: String, details: String) {
        val user = auth.currentUser
        val log = UserLog(
            userId = user?.uid ?: "Anonymous",
            userEmail = user?.email ?: "Anonymous",
            action = action,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addLog(log)
        }
    }
}
