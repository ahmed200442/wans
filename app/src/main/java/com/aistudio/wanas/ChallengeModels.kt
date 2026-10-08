package com.aistudio.wanas

import kotlinx.serialization.Serializable

@Serializable
data class Challenge(
    val id: String,
    val wave_id: String? = null,
    val challenger_id: String,
    val opponent_id: String? = null,
    val title: String,
    val challenge_type: String = "quiz",
    val status: String = "pending",
    val challenger_score: Int = 0,
    val opponent_score: Int = 0,
    val winner_id: String? = null,
    val share_code: String = "",
    val expires_at: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class DailyQuest(
    val id: String,
    val quest_date: String? = null,
    val title: String,
    val description: String,
    val quest_type: String,
    val target_count: Int,
    val xp_reward: Int = 0,
    val coin_reward: Int = 0,
    val is_active: Boolean = true
)

@Serializable
data class Achievement(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val icon: String = "🏆",
    val xp_reward: Int = 0,
    val coin_reward: Int = 0,
    val is_active: Boolean = true
)

@Serializable
data class UserDailyQuest(
    val user_id: String,
    val quest_id: String,
    val progress: Int = 0,
    val completed_at: String? = null
)

@Serializable
data class UserAchievement(
    val user_id: String,
    val achievement_id: String,
    val unlocked_at: String? = null
)
