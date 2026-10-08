package com.aistudio.wanas

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RoomEvent(
    val id: String,
    val room_id: String,
    val user_id: String,
    val event_type: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val created_at: String? = null
)

@Serializable
data class NewRoomEvent(
    val room_id: String,
    val user_id: String,
    val event_type: String,
    val payload: JsonObject = JsonObject(emptyMap())
)
