package com.aistudio.wanas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val loading: Boolean = false,
    val reports: List<Report> = emptyList(),
    val rooms: List<VoiceRoom> = emptyList(),
    val users: List<Profile> = emptyList(),
    val logs: List<AuditLog> = emptyList(),
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val repository = AdminRepository()
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val reports = repository.reports()
                val rooms = repository.liveRooms()
                val users = repository.users()
                val logs = repository.auditLogs()
                _state.value = _state.value.copy(loading = false, reports = reports, rooms = rooms, users = users, logs = logs)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message ?: "تعذر تحميل لوحة الإدارة")
            }
        }
    }

    fun closeRoom(id: String) {
        viewModelScope.launch {
            runCatching { repository.closeRoom(id); refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر إغلاق الغرفة") }
        }
    }

    fun resolveReport(id: String) {
        viewModelScope.launch {
            runCatching { repository.resolveReport(id); refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر حل البلاغ") }
        }
    }
}
