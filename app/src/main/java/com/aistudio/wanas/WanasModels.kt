package com.aistudio.wanas

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val display_name: String,
    val avatar_url: String? = null,
    val bio: String = "",
    val level: Int = 1,
    val xp: Long = 0,
    val streak_days: Int = 0,
    val last_active_at: String? = null,
    val is_online: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable data class Friendship(val id: String,val requester_id: String,val addressee_id: String,val status: String = "pending",val created_at: String? = null,val updated_at: String? = null)
@Serializable data class Conversation(val id: String,val created_at: String? = null,val updated_at: String? = null)
@Serializable data class Message(val id: String,val conversation_id: String,val sender_id: String,val body: String,val message_type: String = "text",val metadata: JsonObject = JsonObject(emptyMap()),val created_at: String? = null,val edited_at: String? = null)
@Serializable data class Gift(val id: String,val name: String,val emoji: String = "🎁",val image_url: String? = null,val price_coins: Long = 100,val is_active: Boolean = true,val sort_order: Int = 0,val created_at: String? = null)
@Serializable data class Wallet(val user_id: String,val coins: Long = 0,val gems: Long = 0,val updated_at: String? = null)
@Serializable data class UserStats(val user_id: String,val wins: Int = 0,val losses: Int = 0,val draws: Int = 0,val total_challenges: Int = 0,val total_buzzes: Int = 0,val current_streak: Int = 0,val best_streak: Int = 0,val weekly_xp: Long = 0,val monthly_xp: Long = 0,val updated_at: String? = null)
@Serializable data class AppNotification(val id: String,val user_id: String,val actor_id: String? = null,val type: String,val title: String,val body: String = "",val data: JsonObject = JsonObject(emptyMap()),val is_read: Boolean = false,val created_at: String? = null)
@Serializable data class Report(val id: String,val reporter_id: String,val target_user_id: String? = null,val challenge_id: String? = null,val reason: String,val details: String = "",val status: String = "open",val created_at: String? = null,val resolved_at: String? = null)
@Serializable data class AdminRole(val user_id: String,val role: String = "moderator",val created_at: String? = null)
@Serializable data class CoinTransaction(val id: String,val user_id: String,val amount: Long,val balance_after: Long,val transaction_type: String,val reference_id: String? = null,val description: String = "",val created_at: String? = null)
