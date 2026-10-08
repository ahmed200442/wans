package com.aistudio.wanas

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

class WanasRepository {
    private val supabase get() = WansSupabase.client
    private val db get() = supabase.postgrest

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    suspend fun profile(): Profile? {
        val id = currentUserId() ?: return null
        return runCatching { db.from("profiles").select { filter { eq("id", id) } }.decodeSingle<Profile>() }.getOrNull()
    }

    suspend fun usersByUsername(username: String): List<Profile> =
        db.from("profiles").select { filter { ilike("username", username) } }.decodeList()

    suspend fun updateProfile(displayName: String, bio: String): Profile {
        val id = currentUserId() ?: error("not_authenticated")
        db.from("profiles").update(ProfileUpdate(displayName, bio)) { filter { eq("id", id) } }
        return profile() ?: error("profile_not_found")
    }

    suspend fun friendships(): List<Friendship> {
        val id = currentUserId() ?: return emptyList()
        return db.from("friendships").select {
            filter {
                or {
                    eq("requester_id", id)
                    eq("addressee_id", id)
                }
            }
        }.decodeList()
    }

    suspend fun sendFriendRequest(receiverId: String) {
        val id = currentUserId() ?: error("not_authenticated")
        db.from("friendships").insert(NewFriendship(id, receiverId))
    }

    suspend fun acceptFriendRequest(friendshipId: String) {
        db.from("friendships").update(FriendshipStatus("accepted")) { filter { eq("id", friendshipId) } }
    }

    suspend fun createConversation(receiverId: String): String =
        db.rpc("mawja_create_conversation", CreateConversationParams(receiverId)).decodeSingle()

    suspend fun messages(conversationId: String): List<Message> =
        db.from("messages").select {
            filter { eq("conversation_id", conversationId) }
        }.decodeList().sortedBy { it.created_at ?: "" }

    suspend fun sendMessage(conversationId: String, body: String) {
        val id = currentUserId() ?: error("not_authenticated")
        db.from("messages").insert(
            NewMessage(conversationId, id, body.trim(), "text", JsonObject(emptyMap()))
        )
    }

    suspend fun markConversationRead(conversationId: String) {
        db.rpc("mawja_mark_conversation_read", MarkReadParams(conversationId))
    }

    suspend fun gifts(): List<Gift> =
        db.from("gifts").select { filter { eq("is_active", true) } }.decodeList().sortedBy { it.sort_order }

    suspend fun wallet(): Wallet? {
        val id = currentUserId() ?: return null
        return runCatching { db.from("wallets").select { filter { eq("user_id", id) } }.decodeSingle<Wallet>() }.getOrNull()
    }

    suspend fun claimDailyCoins(amount: Long = 100): kotlinx.serialization.json.JsonObject =
        db.rpc("wanas_claim_daily_coins", kotlinx.serialization.json.buildJsonObject { put("p_amount", amount) }).decodeSingle()

    suspend fun coinTransactions(): List<CoinTransaction> {
        val id = currentUserId() ?: return emptyList()
        return db.from("coin_transactions").select {
            filter { eq("user_id", id) }
        }.decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun sendGift(receiverId: String, giftId: String, quantity: Int): JsonObject =
        db.rpc("mawja_send_gift", SendGiftParams(receiverId, giftId, quantity)).decodeSingle()

    suspend fun notifications(): List<AppNotification> {
        val id = currentUserId() ?: return emptyList()
        return db.from("notifications").select {
            filter { eq("user_id", id) }
        }.decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun markNotificationRead(id: String) {
        db.from("notifications").update(NotificationRead(true)) { filter { eq("id", id) } }
    }

    suspend fun stats(): UserStats? {
        val id = currentUserId() ?: return null
        return runCatching { db.from("user_stats").select { filter { eq("user_id", id) } }.decodeSingle<UserStats>() }.getOrNull()
    }

    suspend fun adminRole(): AdminRole? {
        val id = currentUserId() ?: return null
        return runCatching { db.from("admin_roles").select { filter { eq("user_id", id) } }.decodeSingle<AdminRole>() }.getOrNull()
    }

    suspend fun reports(): List<Report> {
        val id = currentUserId() ?: return emptyList()
        return db.from("reports").select { filter { eq("reporter_id", id) } }.decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun createReport(targetUserId: String?, reason: String, details: String) {
        val id = currentUserId() ?: error("not_authenticated")
        db.from("reports").insert(NewReport(id, targetUserId, null, reason, details))
    }

    suspend fun sendBuzz(receiverId: String, kind: String = "buzz") {
        val id = currentUserId() ?: error("not_authenticated")
        db.from("buzzes").insert(NewBuzz(id, receiverId, null, kind))
    }

    suspend fun setOnline(online: Boolean) {
        val id = currentUserId() ?: return
        db.from("profiles").update(ProfilePresence(online, null)) { filter { eq("id", id) } }
    }

    @Serializable private data class ProfileUpdate(val display_name: String, val bio: String)
    @Serializable private data class ProfilePresence(val is_online: Boolean, val last_active_at: String?)
    @Serializable private data class NewFriendship(val requester_id: String, val addressee_id: String)
    @Serializable private data class FriendshipStatus(val status: String)
    @Serializable private data class CreateConversationParams(val p_receiver_id: String)
    @Serializable private data class MarkReadParams(val p_conversation_id: String)
    @Serializable private data class NewMessage(
        val conversation_id: String,
        val sender_id: String,
        val body: String,
        val message_type: String,
        val metadata: JsonObject
    )
    @Serializable private data class SendGiftParams(
        val p_receiver_id: String,
        val p_gift_id: String,
        val p_quantity: Int
    )
    @Serializable private data class NotificationRead(val is_read: Boolean)
    @Serializable private data class NewReport(
        val reporter_id: String,
        val target_user_id: String?,
        val challenge_id: String?,
        val reason: String,
        val details: String
    )
    @Serializable private data class NewBuzz(
        val sender_id: String,
        val receiver_id: String,
        val challenge_id: String?,
        val kind: String
    )
}
