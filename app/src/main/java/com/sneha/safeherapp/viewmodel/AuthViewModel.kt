package com.sneha.safeherapp.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.data.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object Unauthenticated : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "SafeHerAuth"

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchUserRole(currentUser.uid)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun fetchUserRole(userId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val document = db.collection("users").document(userId).get().await()
                val role = document.getString("role") ?: "user"
                _authState.value = AuthState.Success(role)
                Log.d(TAG, "Fetched role for user $userId: $role")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch user data for $userId", e)
                _authState.value = AuthState.Error("Failed to fetch user data")
            }
        }
    }

    fun signUp(name: String, email: String, password: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d(TAG, "Starting signup for $email with role $role")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("User creation failed")
                
                val normalizedRole = role.lowercase()
                val user = User(
                    uid = userId, 
                    fullName = name, 
                    email = email, 
                    role = normalizedRole,
                    createdAt = Timestamp.now()
                )
                
                // Save to Firestore. Document ID is the UID.
                db.collection("users").document(userId).set(user).await()
                Log.d(TAG, "User data stored in Firestore for UID: $userId")
                
                _authState.value = AuthState.Success(normalizedRole)
                Log.d(TAG, "Signup successful for $email")
            } catch (e: Exception) {
                Log.e(TAG, "Signup failed for $email", e)
                _authState.value = AuthState.Error(e.localizedMessage ?: "Signup failed")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d(TAG, "Attempting login for $email")
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("Login failed")
                
                val document = db.collection("users").document(userId).get().await()
                val role = document.getString("role") ?: "user"
                
                _authState.value = AuthState.Success(role)
                Log.d(TAG, "Login successful for $email, role: $role")
            } catch (e: Exception) {
                Log.e(TAG, "Login failed for $email", e)
                _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        Log.d(TAG, "User logged out")
    }

    fun resetState() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}
