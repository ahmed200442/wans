package com.aistudio.wanas

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class RoomRepository {
    private val db get() = WansSupabase.client.postgrest

    fun currentUserId(): String? = WansSupabase.client.auth.currentUserOrNull()?.id

    suspend fun join(roomId: String, seat: Int? = null): RoomMember =
        db.rpc("wanas_join_room", buildJsonObject { put("p_room_id", roomId); seat?.let { put("p_seat_index", it) } }).decodeList<RoomMember>().firstOrNull() ?: error("join_room_failed")

    suspend fun leave(roomId: String) { db.rpc("wanas_leave_room", buildJsonObject { put("p_room_id", roomId) }) }

    suspend fun heartbeat(roomId: String) { db.rpc("wanas_room_heartbeat", buildJsonObject { put("p_room_id", roomId) }) }

    suspend fun setMic(roomId: String, on: Boolean, speaking: Boolean = false) {
        db.rpc("wanas_set_room_mic", buildJsonObject { put("p_room_id", roomId); put("p_on", on); put("p_is_speaking", speaking) })
    }

    suspend fun takeSeat(roomId: String, seat: Int) {
        db.rpc("wanas_take_room_seat", buildJsonObject { put("p_room_id", roomId); put("p_seat_index", seat) })
    }

    suspend fun events(roomId: String): List<RoomEvent> =
        db.from("room_events").select {
            filter { eq("room_id", roomId) }
        }.decodeList<RoomEvent>().sortedBy { it.created_at ?: "" }

    suspend fun sendEvent(roomId: String, eventType: String, payload: JsonObject = JsonObject(emptyMap())) {
        val user = currentUserId() ?: error("not_authenticated")
        db.from("room_events").insert(NewRoomEvent(roomId, user, eventType, payload))
    }

    suspend fun members(roomId: String): List<RoomMember> =
        db.from("room_members").select {
            filter {
                eq("room_id", roomId)
            }
        }.decodeList<RoomMember>()

    suspend fun createRoom(title: String, category: String, description: String = ""): VoiceRoom {
        val owner = currentUserId() ?: error("not_authenticated")
        return db.from("rooms").insert(
            NewRoom(owner, title.trim(), description, category, "live", 100)
        ) {
            select()
        }.decodeList<VoiceRoom>().firstOrNull() ?: error("create_room_failed")
    }

    suspend fun muteMember(roomId: String, userId: String, muted: Boolean) {
        db.rpc("wanas_set_member_mute", buildJsonObject { put("p_room_id", roomId); put("p_user_id", userId); put("p_muted", muted) })
    }

    suspend fun kickMember(roomId: String, userId: String) {
        db.rpc("wanas_kick_member", buildJsonObject { put("p_room_id", roomId); put("p_user_id", userId) })
    }

    suspend fun muteAll(roomId: String) {
        db.rpc("wanas_mute_all_room_speakers", buildJsonObject { put("p_room_id", roomId) })
    }

    suspend fun closeRoom(roomId: String) {
        db.from("rooms").update(CloseRoom("ended")) {
            filter { eq("id", roomId) }
        }
    }

    suspend fun liveRooms(): List<VoiceRoom> =
        db.from("rooms").select {
            filter { eq("status", "live") }
        }.decodeList<VoiceRoom>()
}

@Serializable private data class NewRoom(
    val owner_id: String,
    val title: String,
    val description: String,
    val category: String,
    val status: String,
    val max_members: Int
)

@Serializable private data class CloseRoom(val status: String)

@Serializable private data class MemberMuteParams(
    val p_room_id: String,
    val p_user_id: String,
    val p_muted: Boolean
)

@Serializable private data class MemberActionParams(
    val p_room_id: String,
    val p_user_id: String
)

@Serializable private data class RoomOnlyParams(val p_room_id: String)
