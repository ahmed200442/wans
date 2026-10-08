package com.aistudio.wanas

import kotlinx.serialization.Serializable

@Serializable
data class RoomMember(
    val room_id: String,
    val user_id: String,
    val role: String = "listener",
    val is_muted: Boolean = false,
    val seat_index: Int? = null,
    val is_microphone_on: Boolean = false,
    val is_speaking: Boolean = false,
    val last_seen_at: String? = null
)

@Serializable data class JoinRoomParams(val p_room_id: String, val p_seat_index: Int? = null)
@Serializable data class RoomIdParams(val p_room_id: String)
@Serializable data class MicParams(val p_room_id: String, val p_on: Boolean, val p_is_speaking: Boolean = false)
@Serializable data class SeatParams(val p_room_id: String, val p_seat_index: Int)
