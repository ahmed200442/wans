package com.aistudio.wanas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WansApp() }
    }
}

private enum class Tab(val title: String) {
    HOME("الرئيسية"), ROOMS("الغرف"), FRIENDS("الأصدقاء"), STORE("المتجر"), PROFILE("حسابي")
}

@Composable
private fun WansApp(authVm: AuthViewModel = viewModel(), roomVm: WansRoomViewModel = viewModel()) {
    val auth by authVm.state.collectAsState()
    val room by roomVm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            if (!auth.signedIn) AuthScreen(auth.busy, auth.error, authVm::signIn, authVm::signUp)
            else {
                LaunchedEffect(Unit) { roomVm.refreshRooms() }
                if (room.joined) RoomStage(room, roomVm::leave, roomVm::toggleMic, roomVm::takeSeat)
                else MainShell(tab, { tab = it }, room, roomVm, authVm::signOut)
            }
        }
    }
}

@Composable
private fun AuthScreen(busy: Boolean, error: String?, onSignIn: (String,String)->Unit, onSignUp: (String,String)->Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.Center) {
        Text("وَنَس", style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold)
        Text("مجتمع صوتي اجتماعي — أونلاين", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(email,{email=it},label={Text("البريد الإلكتروني")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(enabled=!busy,onClick={onSignIn(email.trim(),password)}) { Text("دخول") }
            OutlinedButton(enabled=!busy,onClick={onSignUp(email.trim(),password)}) { Text("حساب جديد") }
        }
        if(error!=null){Spacer(Modifier.height(12.dp));Text(error,color=MaterialTheme.colorScheme.error)}
    }
}

@Composable
private fun MainShell(tab: Tab,onTab:(Tab)->Unit,state:RoomUiState,vm:WansRoomViewModel,onSignOut:()->Unit){
    Scaffold(bottomBar={
        NavigationBar {
            Tab.entries.forEach { t ->
                NavigationBarItem(selected=tab==t,onClick={onTab(t)},icon={Text(t.title.take(1))},label={Text(t.title)})
            }
        }
    }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Text("وَنَس",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                TextButton(onClick=onSignOut){Text("خروج")}
            }
            when(tab){
                Tab.HOME -> HomeTab(state,vm)
                Tab.ROOMS -> RoomsTab(state,vm)
                Tab.FRIENDS -> SimpleSection("الأصدقاء","الأصدقاء والطلبات والمحادثات",listOf("المتصلون الآن","طلبات الصداقة","المحادثات","الرسائل الصوتية"))
                Tab.STORE -> SimpleSection("المتجر","العملات والهدايا والمكافآت",listOf("شراء العملات","الهدايا","المكافآت اليومية","العضوية المميزة"))
                Tab.PROFILE -> SimpleSection("حسابي","الملف الشخصي والإحصائيات",listOf("الاسم والصورة","المستوى والخبرة","الخصوصية والأمان"))
            }
        }
    }
}

@Composable private fun HomeTab(state:RoomUiState,vm:WansRoomViewModel){
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(14.dp))
        Text("أهلًا بك في وَنَس 👋",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text("الغرف النشطة تظهر هنا تلقائيًا.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("غرف مباشرة",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            TextButton(onClick=vm::refreshRooms){Text("تحديث")}
        }
        if(state.rooms.isEmpty()) EmptyCard("لا توجد غرف مباشرة حاليًا")
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(state.rooms,key={it.id}){RoomCard(it){vm.join(it.id,null)}}}
    }
}

@Composable private fun RoomsTab(state:RoomUiState,vm:WansRoomViewModel){
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(14.dp))
        Text("الغرف الصوتية",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text("استكشف الغرف المتاحة وانضم مباشرة.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(state.rooms,key={it.id}){RoomCard(it){vm.join(it.id,null)}}}
    }
}

@Composable private fun RoomCard(room:VoiceRoom,onJoin:()->Unit){
    Card(Modifier.fillMaxWidth().clickable(onClick=onJoin),shape=RoundedCornerShape(18.dp)){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text("🎙")}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){
                Text(room.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Text(room.category+" • حتى "+room.max_members+" عضو",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick=onJoin){Text("دخول")}
        }
    }
}

@Composable private fun RoomStage(state:RoomUiState,onLeave:()->Unit,onMic:()->Unit,onSeat:(Int)->Unit){
    Column(Modifier.fillMaxSize().padding(14.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Column{
                Text("الغرفة المباشرة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text(state.members.size.toString()+" عضو متصل")
            }
            TextButton(onClick=onLeave){Text("مغادرة")}
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){
            Column(Modifier.padding(14.dp)){
                Text("المقاعد",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                for(row in 0 until 2){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                        for(col in 0 until 4){
                            val seat=row*4+col
                            val member=state.members.firstOrNull{it.seat_index==seat}
                            SeatItem(seat,member){onSeat(seat)}
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("الأعضاء",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
            items(state.members,key={it.room_id+it.user_id}){m->
                ListItem(
                    headlineContent={Text(m.user_id.take(12))},
                    supportingContent={Text(m.role+" • "+if(m.is_microphone_on)"🎙 يتحدث" else "مستمع")},
                    trailingContent={if(m.is_muted)Text("🔇") else Text("")}
                )
            }
        }
        Button(onClick=onMic,Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text(if(state.micOn)"إيقاف المايك" else "تشغيل المايك")}
        if(state.error!=null)Text(state.error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(6.dp))
    }
}

@Composable private fun SeatItem(index:Int,member:RoomMember?,onClick:()->Unit){
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick).padding(4.dp)){
        Box(Modifier.size(48.dp).clip(CircleShape).background(if(member==null)MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(if(member==null)"+" else "👤")}
        Text(if(member==null)"مقعد "+(index+1) else "متحدث",style=MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun SimpleSection(title:String,subtitle:String,items:List<String>){
    Column(Modifier.fillMaxSize().padding(top=18.dp)){
        Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        items.forEach{Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){ListItem(headlineContent={Text(it)},trailingContent={Text("›")})}}
    }
}
@Composable private fun EmptyCard(text:String){Card(Modifier.fillMaxWidth()){Box(Modifier.padding(28.dp),contentAlignment=Alignment.Center){Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
