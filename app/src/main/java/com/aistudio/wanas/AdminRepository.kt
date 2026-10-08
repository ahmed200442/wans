package com.aistudio.wanas

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class AuditLog(
    val id: String,
    val admin_id: String,
    val action: String,
    val target_type: String? = null,
    val target_id: String? = null,
    val metadata: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap()),
    val created_at: String? = null
)

class AdminRepository {
    private val db get() = WansSupabase.client.postgrest

    private suspend fun requireAdmin() {
        val id = WansSupabase.client.auth.currentUserOrNull()?.id ?: error("not_authenticated")
        db.from("admin_roles").select { filter { eq("user_id", id) } }.decodeSingleOrNull<AdminRole>()
            ?: error("admin_only")
    }

    suspend fun reports(): List<Report> {
        requireAdmin()
        return db.from("reports").select().decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun liveRooms(): List<VoiceRoom> {
        requireAdmin()
        return db.from("rooms").select { filter { eq("status", "live") } }.decodeList()
    }

    suspend fun users(limit: Int = 100): List<Profile> {
        requireAdmin()
        return db.from("profiles").select { limit(limit) }.decodeList()
    }

    suspend fun auditLogs(limit: Int = 100): List<AuditLog> {
        requireAdmin()
        return db.from("audit_logs").select { limit(limit) }.decodeList().sortedByDescending { it.created_at ?: "" }
    }

    suspend fun closeRoom(roomId: String) {
        requireAdmin()
        db.from("rooms").update(AdminRoomClose("ended", null)) { filter { eq("id", roomId) } }
    }

    suspend fun resolveReport(reportId: String) {
        requireAdmin()
        db.from("reports").update(AdminReportResolve("resolved", "now")) { filter { eq("id", reportId) } }
    }

    @Serializable private data class AdminRoomClose(val status: String, val ended_at: String?)
    @Serializable private data class AdminReportResolve(val status: String, val resolved_at: String)
}
