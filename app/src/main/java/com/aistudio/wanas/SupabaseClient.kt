package com.aistudio.wanas

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object WansSupabase {
    val client = createSupabaseClient(
        supabaseUrl = "https://txpkrctvdtjcptotfqer.supabase.co",
        supabaseKey = "sb_publishable_cBct3a8MRvDVgihvChwaaw_Eh2LT4H2"
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
