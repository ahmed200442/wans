package com.aistudio.wanas

import io.github.jan.supabase.postgrest.from

class ChallengeRepository {
    private val db get() = WansSupabase.client.postgrest

    suspend fun createChallenge(opponentId: String, title: String, type: String = "quiz") {
        val id = WansSupabase.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        db.from("challenges").insert(NewChallenge(id,opponentId,title.trim(),type))
    }

    suspend fun myChallenges(): List<Challenge> {
        val id = WansSupabase.client.auth.currentUserOrNull()?.id ?: return emptyList()
        return db.from("challenges").select {
            filter {
                or {
                    eq("challenger_id", id)
                    eq("opponent_id", id)
                }
            }
        }.decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun dailyQuests(): List<DailyQuest> =
        db.from("daily_quests").select { filter { eq("is_active", true) } }.decodeList()

    suspend fun achievements(): List<Achievement> =
        db.from("achievements").select { filter { eq("is_active", true) } }.decodeList()

    suspend fun myQuestProgress(): List<UserDailyQuest> {
        val id = WansSupabase.client.auth.currentUserOrNull()?.id ?: return emptyList()
        return db.from("user_daily_quests").select { filter { eq("user_id", id) } }.decodeList()
    }

    suspend fun myAchievements(): List<UserAchievement> {
        val id = WansSupabase.client.auth.currentUserOrNull()?.id ?: return emptyList()
        return db.from("user_achievements").select { filter { eq("user_id", id) } }.decodeList()
    }
}
\n@kotlinx.serialization.Serializable private data class NewChallenge(val challenger_id:String,val opponent_id:String,val title:String,val challenge_type:String)\n