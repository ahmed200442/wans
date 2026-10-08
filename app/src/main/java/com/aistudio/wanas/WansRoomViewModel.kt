package com.aistudio.wanas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RoomUiState(
    val roomId: String = "",
    val joined: Boolean = false,
    val micOn: Boolean = false,
    val seat: Int? = null,
    val members: List<RoomMember> = emptyList(),
    val error: String? = null,
    val busy: Boolean = false
)

class WansRoomViewModel : ViewModel() {
    private val repository = RoomRepository()
    private val _state = MutableStateFlow(RoomUiState())
    val state: StateFlow<RoomUiState> = _state.asStateFlow()
    private var heartbeatJob: Job? = null
    private var membersJob: Job? = null

    fun join(roomId: String, seat: Int? = null) {
        if (roomId.isBlank() || _state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(roomId=roomId, busy=true, error=null)
            runCatching { repository.join(roomId, seat) }
                .onSuccess { member ->
                    _state.value = _state.value.copy(joined=true, busy=false, seat=member.seat_index)
                    startSync(roomId)
                }
                .onFailure { e -> _state.value = _state.value.copy(busy=false, error=e.message ?: "فشل دخول الغرفة") }
        }
    }

    fun leave() {
        val roomId = _state.value.roomId
        if (!_state.value.joined) return
        viewModelScope.launch {
            runCatching { repository.leave(roomId) }
            heartbeatJob?.cancel(); membersJob?.cancel()
            _state.value = RoomUiState()
        }
    }

    fun toggleMic() {
        val current = _state.value
        if (!current.joined) return
        viewModelScope.launch {
            val next = !current.micOn
            runCatching { repository.setMic(current.roomId, next) }
                .onSuccess { _state.value = _state.value.copy(micOn=next, error=null) }
                .onFailure { e -> _state.value = _state.value.copy(error=e.message ?: "تعذر تغيير المايك") }
        }
    }

    fun takeSeat(seat: Int) {
        val roomId = _state.value.roomId
        if (!_state.value.joined) return
        viewModelScope.launch {
            runCatching { repository.takeSeat(roomId, seat) }
                .onSuccess { _state.value = _state.value.copy(seat=seat, error=null) }
                .onFailure { e -> _state.value = _state.value.copy(error=e.message ?: "المقعد غير متاح") }
        }
    }

    private fun startSync(roomId: String) {
        heartbeatJob?.cancel(); membersJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) { delay(30_000); runCatching { repository.heartbeat(roomId) } }
        }
        membersJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.members(roomId) }.onSuccess { list ->
                    _state.value = _state.value.copy(members=list)
                }
                delay(2_000)
            }
        }
    }

    override fun onCleared() {
        heartbeatJob?.cancel(); membersJob?.cancel(); super.onCleared()
    }
}
