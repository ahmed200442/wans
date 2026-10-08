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
}
