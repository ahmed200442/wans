package com.aistudio.wanas

import io.github.jan.supabase.postgrest.from

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

    suspend fun members(roomId: String): List<RoomMember> =
        db.from("room_members").select {
            filter {
                eq("room_id", roomId)
                is_("left_at", null)
            }
        }.decodeList()

    suspend fun createRoom(title: String, category: String, description: String = ""): VoiceRoom {
        val owner = WansSupabase.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        return db.from("rooms").insert(NewRoom(owner, title.trim(), description, category, "live", 100)) {
            select()
        }.decodeSingle()
    }

    suspend fun closeRoom(roomId: String) {
        db.from("rooms").update(CloseRoom("ended")) { filter { eq("id", roomId) } }
    }

    suspend fun liveRooms(): List<VoiceRoom> =
        db.from("rooms").select { filter { eq("status", "live") } }.decodeList()
}
