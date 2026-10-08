package com.aistudio.wanas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WansApp() }
    }
}

@Composable
private fun WansApp(authVm: AuthViewModel = viewModel(), roomVm: WansRoomViewModel = viewModel()) {
    val auth by authVm.state.collectAsState()
    val room by roomVm.state.collectAsState()
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            if (!auth.signedIn) AuthScreen(auth.busy, auth.error, authVm::signIn, authVm::signUp)
            else RoomScreen(room, roomVm::join, roomVm::leave, roomVm::toggleMic, roomVm::takeSeat, authVm::signOut)
        }
    }
}

@Composable
private fun AuthScreen(busy: Boolean, error: String?, onSignIn: (String,String)->Unit, onSignUp: (String,String)->Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.Center) {
        Text("Wans", style=MaterialTheme.typography.headlineLarge)
        Text("تسجيل الدخول للغرف أونلاين")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(email, {email=it}, label={Text("البريد الإلكتروني")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(password, {password=it}, label={Text("كلمة المرور")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(enabled=!busy, onClick={onSignIn(email.trim(),password)}) { Text("دخول") }
            OutlinedButton(enabled=!busy, onClick={onSignUp(email.trim(),password)}) { Text("حساب جديد") }
        }
        if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, color=MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun RoomScreen(
    state: RoomUiState,
    join: (String,Int?)->Unit,
    leave: ()->Unit,
    toggleMic: ()->Unit,
    takeSeat: (Int)->Unit,
    signOut: ()->Unit
) {
    var roomId by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Wans — الغرفة أونلاين", style=MaterialTheme.typography.titleLarge)
            TextButton(onClick=signOut) { Text("خروج") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(roomId, {roomId=it}, label={Text("معرّف الغرفة UUID")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(enabled=!state.joined && !state.busy, onClick={ { join(roomId.trim(), null) } }) { Text("دخول الغرفة") }
            OutlinedButton(enabled=state.joined, onClick=leave) { Text("مغادرة") }
            Button(enabled=state.joined, onClick=toggleMic) { Text(if(state.micOn) "إغلاق المايك" else "تشغيل المايك") }
        }
        if (state.joined) {
            Spacer(Modifier.height(10.dp))
            Text("الأعضاء الآن: " + state.members.size)
            Text("مقعدك: " + (state.seat ?: "مستمع"))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(state.members, key={ it.room_id + ":" + it.user_id }) { m ->
                    Card(Modifier.fillMaxWidth().padding(vertical=3.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.SpaceBetween) {
                            Column {
                                Text(m.user_id)
                                Text(m.role + " • " + if(m.is_microphone_on) "🎙️ يتكلم" else "مستمع")
                            }
                            if (m.seat_index != null) Text("مقعد " + m.seat_index)
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(24.dp))
            Text("بعد الدخول ستظهر الأعضاء الموجودين في الغرفة مباشرة.")
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error, color=MaterialTheme.colorScheme.error)
        }
    }
}
