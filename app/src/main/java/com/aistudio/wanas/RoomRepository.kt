package com.aistudio.wanas

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

class RoomRepository {
    private val db get() = WansSupabase.client.postgrest

    suspend fun join(roomId: String, seat: Int? = null): RoomMember =
        db.rpc("wanas_join_room", JoinRoomParams(roomId, seat)).decodeSingle()

    suspend fun leave(roomId: String) { db.rpc("wanas_leave_room", RoomIdParams(roomId)) }
    suspend fun heartbeat(roomId: String) { db.rpc("wanas_room_heartbeat", RoomIdParams(roomId)) }
    suspend fun setMic(roomId: String, on: Boolean, speaking: Boolean = false) {
        db.rpc("wanas_set_room_mic", MicParams(roomId, on, speaking))
    }
    suspend fun takeSeat(roomId: String, seat: Int) {
        db.rpc("wanas_take_room_seat", SeatParams(roomId, seat))
    }

    suspend fun events(roomId: String): List<RoomEvent> = db.from("room_events").select { filter { eq("room_id", roomId) } }.decodeList().sortedBy { it.created_at ?: "" }

    suspend fun sendEvent(roomId: String, eventType: String, payload: JsonObject = JsonObject(emptyMap())) {
        val user = WansSupabase.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        db.from("room_events").insert(NewRoomEvent(roomId, user, eventType, payload))
    }

    suspend fun members(roomId: String): List<RoomMember> =
        db.from("room_members").select {
            filter {
                eq("room_id", roomId)
                eq("left_at", null)
            }
        }.decodeList()

    suspend fun createRoom(title: String, category: String, description: String = ""): VoiceRoom {
        val owner = WansSupabase.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        return db.from("rooms").insert(NewRoom(owner, title.trim(), description, category, "live", 100)) {
            select()
        }.decodeSingle()
    }

    suspend fun muteMember(roomId: String,userId: String,muted: Boolean) { db.rpc("wanas_set_member_mute", MemberMuteParams(roomId,userId,muted)) }
    suspend fun kickMember(roomId: String,userId: String) { db.rpc("wanas_kick_member", MemberActionParams(roomId,userId)) }
    suspend fun muteAll(roomId: String) { db.rpc("wanas_mute_all_room_speakers", RoomOnlyParams(roomId)) }

    suspend fun closeRoom(roomId: String) {
        db.from("rooms").update(CloseRoom("ended")) { filter { eq("id", roomId) } }
    }

    suspend fun liveRooms(): List<VoiceRoom> =
        db.from("rooms").select { filter { eq("status", "live") } }.decodeList()
}
\n@Serializable private data class NewRoom(val owner_id:String,val title:String,val description:String,val category:String,val status:String,val max_members:Int)\n@Serializable private data class CloseRoom(val status:String)\n@Serializable private data class MemberMuteParams(val p_room_id:String,val p_user_id:String,val p_muted:Boolean)\n@Serializable private data class MemberActionParams(val p_room_id:String,val p_user_id:String)\n@Serializable private data class RoomOnlyParams(val p_room_id:String)\n