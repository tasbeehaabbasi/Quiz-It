package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    
    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun signIn(email: String, password: String) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters long."
            return
        }
        
        viewModelScope.launch {
            _errorMessage.value = null
            val result = authRepository.signInWithEmailAndPassword(cleanEmail, password)
            result.onFailure { exception ->
                _errorMessage.value = mapFirebaseAuthException(exception)
            }
        }
    }

    fun signUp(email: String, password: String) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            _errorMessage.value = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters long."
            return
        }
        
        viewModelScope.launch {
            _errorMessage.value = null
            val result = authRepository.createUserWithEmailAndPassword(cleanEmail, password)
            result.onFailure { exception ->
                _errorMessage.value = mapFirebaseAuthException(exception)
            }
        }
    }
    
    private fun mapFirebaseAuthException(exception: Throwable): String {
        Log.e("AuthViewModel", "Authentication Error: ${exception.message}", exception)
        
        if (exception is FirebaseAuthException) {
            Log.e("AuthViewModel", "Raw Firebase Auth Error Code: ${exception.errorCode}")
            return when (exception.errorCode) {
                "ERROR_INVALID_CREDENTIAL", "INVALID_LOGIN_CREDENTIALS" -> "Incorrect email or password. Please try again."
                "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
                "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use a stronger password."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a few minutes."
                "ERROR_USER_DISABLED" -> "This account has been disabled."
                else -> exception.message ?: "Authentication failed."
            }
        }
        
        if (exception.message?.contains("auth credential is incorrect", ignoreCase = true) == true) {
            return "Incorrect email or password. Please try again."
        }
        
        return exception.message ?: "Authentication failed."
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun signOut() {
        authRepository.signOut()
    }
}
