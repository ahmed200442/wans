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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class RoomUiState(
    val roomId: String = "",
    val joined: Boolean = false,
    val micOn: Boolean = false,
    val seat: Int? = null,
    val members: List<RoomMember> = emptyList(),
    val error: String? = null,
    val busy: Boolean = false,
    val isOwner: Boolean = false,
    val rooms: List<VoiceRoom> = emptyList(),
    val events: List<RoomEvent> = emptyList()
)

class WansRoomViewModel : ViewModel() {
    fun currentUserId(): String? = repository.currentUserId()
    private val repository = RoomRepository()
    private val _state = MutableStateFlow(RoomUiState())
    val state: StateFlow<RoomUiState> = _state.asStateFlow()
    private var heartbeatJob: Job? = null
    private var membersJob: Job? = null
    private var roomsJob: Job? = null

    fun createRoom(title: String, category: String, description: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { repository.createRoom(title, category, description) }
                .onSuccess { room -> _state.value = _state.value.copy(busy = false); join(room.id, null); refreshRooms() }
                .onFailure { e -> _state.value = _state.value.copy(busy = false, error = e.message ?: "تعذر إنشاء الغرفة") }
        }
    }

    fun closeCreatedRoom() {
        val id = _state.value.roomId
        if (id.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.closeRoom(id) }
            leave()
            refreshRooms()
        }
    }

    fun refreshRooms() {
        viewModelScope.launch {
            runCatching { repository.liveRooms() }.onSuccess { _state.value = _state.value.copy(rooms = it) }
        }
    }

    fun join(roomId: String, seat: Int? = null) {
        if (roomId.isBlank() || _state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(roomId=roomId, busy=true, error=null)
            runCatching { repository.join(roomId, seat) }
                .onSuccess { member ->
                    _state.value = _state.value.copy(joined=true, busy=false, seat=member.seat_index, isOwner=member.role=="owner" || member.role=="host")
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

    fun muteMember(userId: String, muted: Boolean) {
        val roomId = _state.value.roomId
        if (!_state.value.isOwner) return
        viewModelScope.launch {
            runCatching { repository.muteMember(roomId, userId, muted) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر تغيير الكتم") }
        }
    }

    fun kickMember(userId: String) {
        val roomId = _state.value.roomId
        if (!_state.value.isOwner) return
        viewModelScope.launch {
            runCatching { repository.kickMember(roomId, userId) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر طرد العضو") }
        }
    }

    fun sendRoomChat(text: String) {
        if (text.isBlank() || !_state.value.joined) return
        viewModelScope.launch { runCatching { repository.sendEvent(_state.value.roomId,"chat",buildJsonObject{put("text",text.trim())}) }.onFailure { _state.value=_state.value.copy(error=it.message ?: "تعذر إرسال الرسالة") } }
    }

    fun sendReaction(reaction: String) {
        if (!_state.value.joined) return
        viewModelScope.launch { runCatching { repository.sendEvent(_state.value.roomId,"reaction",buildJsonObject{put("value",reaction)}) } }
    }

    fun raiseHand() {
        if (!_state.value.joined) return
        viewModelScope.launch { runCatching { repository.sendEvent(_state.value.roomId,"raised_hand") } }
    }

    fun togglePkBattle() {
        if (!_state.value.isOwner) return
        viewModelScope.launch { runCatching { repository.sendEvent(_state.value.roomId,"pk",buildJsonObject{put("active",true)}) } }
    }

    fun muteAll() {
        val roomId = _state.value.roomId
        if (!_state.value.isOwner) return
        viewModelScope.launch {
            runCatching { repository.muteAll(roomId) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر كتم الجميع") }
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
        heartbeatJob?.cancel(); membersJob?.cancel(); roomsJob?.cancel()
        roomsJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.liveRooms() }.onSuccess { _state.value = _state.value.copy(rooms = it) }
                delay(5000)
            }
        }
        heartbeatJob = viewModelScope.launch {
            while (isActive) { delay(30_000); runCatching { repository.heartbeat(roomId) } }
        }
        membersJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.members(roomId) }.onSuccess { list -> _state.value = _state.value.copy(members=list) }
                runCatching { repository.events(roomId) }.onSuccess { events -> _state.value = _state.value.copy(events=events.takeLast(100)) }
                delay(2_000)
            }
        }
    }

    override fun onCleared() {
        heartbeatJob?.cancel(); membersJob?.cancel(); roomsJob?.cancel(); super.onCleared()
    }
}
