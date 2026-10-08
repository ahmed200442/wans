package com.aistudio.wanas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AuthUiState(val signedIn: Boolean = false, val busy: Boolean = false, val error: String? = null)

class AuthViewModel : ViewModel() {
    private val auth get() = WansSupabase.client.auth
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init { _state.value = AuthUiState(signedIn = auth.currentUserOrNull() != null) }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(busy = true)
            runCatching { auth.signInWith(Email) { this.email = email; this.password = password } }
                .onSuccess { _state.value = AuthUiState(signedIn = true) }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "فشل تسجيل الدخول") }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(busy = true)
            runCatching { auth.signUpWith(Email) { this.email = email; this.password = password; data = buildJsonObject { put("username", username); put("display_name", username) } } }
                .onSuccess { _state.value = AuthUiState(signedIn = auth.currentUserOrNull() != null, error = if (auth.currentUserOrNull() == null) "راجع بريدك لتأكيد الحساب" else null) }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "فشل إنشاء الحساب") }
        }
    }

    fun signOut() {
        viewModelScope.launch { runCatching { auth.signOut() }; _state.value = AuthUiState() }
    }
}
