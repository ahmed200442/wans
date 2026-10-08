package com.aistudio.wanas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WansApp() }
    }
}

private enum class Tab(val title: String) {
    HOME("الرئيسية"), ROOMS("الغرف"), FRIENDS("الأصدقاء"), CHAT("المحادثة"), STORE("المتجر"), PROFILE("حسابي")
}

@Composable
private fun WansApp(authVm: AuthViewModel = viewModel(), roomVm: WansRoomViewModel = viewModel(), appVm: WanasViewModel = viewModel()) {
    val auth by authVm.state.collectAsState()
    val room by roomVm.state.collectAsState()
    val app by appVm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }

    LaunchedEffect(app.activeConversationId) { if (app.activeConversationId != null) tab = Tab.CHAT }

    LaunchedEffect(auth.signedIn) {
        if (auth.signedIn) appVm.loadAll()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9A7BFF),
            secondary = Color(0xFF5EE7D2),
            background = Color(0xFF080A10),
            surface = Color(0xFF11141D)
        )
    ) {
        Surface(Modifier.fillMaxSize()) {
            if (!auth.signedIn) AuthScreen(auth.busy, auth.error, authVm::signIn, authVm::signUp)
            else if (room.joined) RoomStage(room, roomVm)
            else MainShell(tab, { tab = it }, room, roomVm, app, appVm, authVm::signOut)
        }
    }
}

@Composable
private fun AuthScreen(busy: Boolean, error: String?, onSignIn: (String,String)->Unit, onSignUp: (String,String,String)->Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.Center) {
        Text("وَنَس", style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold)
        Text("مجتمع صوتي اجتماعي — أونلاين", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(username,{username=it},label={Text("اسم المستخدم")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(email,{email=it},label={Text("البريد الإلكتروني")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password,{password=it},label={Text("كلمة المرور")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation())
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(enabled=!busy,onClick={onSignIn(email.trim(),password)}) { Text("دخول") }
            OutlinedButton(enabled=!busy && username.isNotBlank(),onClick={onSignUp(email.trim(),password,username.trim())}) { Text("حساب جديد") }
        }
        if(error!=null){Spacer(Modifier.height(12.dp));Text(error,color=MaterialTheme.colorScheme.error)}
    }
}

@Composable
private fun MainShell(tab: Tab,onTab:(Tab)->Unit,roomState:RoomUiState,roomVm:WansRoomViewModel,state:WanasUiState,appVm:WanasViewModel,signOut:()->Unit){
    Scaffold(bottomBar={
        NavigationBar { Tab.entries.forEach { t ->
            NavigationBarItem(selected=tab==t,onClick={onTab(t)},icon={Text(t.title.take(1))},label={Text(t.title)})
        }}
    }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(horizontal=14.dp)) {
            Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Column {
                    Text("وَنَس",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    state.profile?.let { Text("@"+it.username,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Row(verticalAlignment=Alignment.CenterVertically){
                    TextButton(onClick={onTab(Tab.STORE)}) { Text("🪙 "+(state.wallet?.coins?:0)) }
                    val unread=state.notifications.count{!it.is_read}
                    if(unread>0) Text("🔔 "+unread,style=MaterialTheme.typography.labelMedium)
                }
            }
            when(tab){
                Tab.HOME -> HomeTab(roomState,roomVm,state,appVm)
                Tab.ROOMS -> RoomsTab(roomState,roomVm)
                Tab.FRIENDS -> FriendsTab(state,appVm)
                Tab.CHAT -> ChatTab(state,appVm)
                Tab.STORE -> StoreTab(state,appVm)
                Tab.PROFILE -> ProfileTab(state,appVm,signOut)
            }
        }
    }
}

@Composable private fun HomeTab(roomState:RoomUiState,roomVm:WansRoomViewModel,state:WanasUiState,appVm:WanasViewModel){
    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxSize()){
        item{
            Spacer(Modifier.height(8.dp))
            Card(shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(18.dp)){
                    Text("أهلًا بك في وَنَس 👋",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text("تحدّث، تعرّف على أصدقاء، وادخل الغرف المباشرة.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        AssistChip(onClick={roomVm.refreshRooms()},label={Text("تحديث الغرف")})
                        AssistChip(onClick={appVm::loadAll},label={Text("تحديث حسابي")})
                    }
                }
            }
        }
        item{SectionTitle("غرف مباشرة")}
        if(roomState.rooms.isEmpty()) item{EmptyCard("لا توجد غرف مباشرة حاليًا")} else items(roomState.rooms,key={it.id}){RoomCard(it){roomVm.join(it.id,null)}}
        item{SectionTitle("مهام اليوم")}
        items(state.dailyQuests.take(3),key={it.id}){q->Card(Modifier.fillMaxWidth()){ListItem(headlineContent={Text("🎯 "+q.title)},supportingContent={Text(q.description+" • الهدف "+q.target_count)},trailingContent={Text("+"+q.coin_reward+" 🪙")})}}
        item{SectionTitle("إشعارات حديثة")}
        items(state.notifications.take(5),key={it.id}){n->NotificationCard(n){appVm.markNotificationRead(n.id)}}
    }
}

@Composable private fun RoomsTab(state:RoomUiState,vm:WansRoomViewModel){
    var showCreate by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()){
        Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Column{Text("الغرف الصوتية",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("الغرف متزامنة مع السيرفر",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Row{TextButton(onClick=vm::refreshRooms){Text("تحديث")};Button(onClick={showCreate=true}){Text("إنشاء")}}
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(state.rooms,key={it.id}){RoomCard(it){vm.join(it.id,null)}}}
    }
    if(showCreate) CreateRoomDialog({showCreate=false}){title,cat->showCreate=false;vm.createRoom(title,cat)}
}

@Composable private fun FriendsTab(state:WanasUiState,appVm:WanasViewModel){
    var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(8.dp))
        Text("الأصدقاء",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        OutlinedTextField(q,{q=it},label={Text("ابحث باسم المستخدم")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Spacer(Modifier.height(8.dp))
        Button(onClick={appVm.searchUser(q)},enabled=q.isNotBlank()){Text("بحث")}
        if(state.users.isNotEmpty()){
            SectionTitle("نتائج البحث")
            state.users.forEach{u->ProfileActionCard(u,{appVm.sendFriendRequest(u.id)},{appVm.openConversation(u.id)},{appVm.sendBuzz(u.id)})}
        }
        SectionTitle("طلبات الصداقة")
        val current=appVm.currentUserId()
        val pending=state.friends.filter{it.status=="pending" && it.addressee_id==current}
        if(pending.isEmpty()) Text("لا توجد طلبات معلقة",color=MaterialTheme.colorScheme.onSurfaceVariant)
        else pending.forEach{f->Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
            Text(f.requester_id.take(16),Modifier.weight(1f));Button(onClick={appVm.acceptFriendRequest(f.id)}){Text("قبول")}
        }}
        SectionTitle("الأصدقاء")
        state.friends.filter{it.status=="accepted"}.forEach{f->Text("• "+friendLabel(f,current),Modifier.padding(vertical=4.dp))}
    }
}

@Composable private fun ChatTab(state:WanasUiState,appVm:WanasViewModel){
    var body by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(8.dp))
        Text("المحادثات",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Text(if(state.activeConversationId==null)"افتح محادثة من تبويب الأصدقاء." else "محادثة نشطة",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
            items(state.messages,key={it.id}){m->
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(10.dp)){Text(m.body);Text(m.sender_id.take(12),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            }
        }
        if(state.activeConversationId!=null){
            Row(verticalAlignment=Alignment.CenterVertically){
                OutlinedTextField(body,{body=it},modifier=Modifier.weight(1f),singleLine=true,label={Text("رسالة")})
                Spacer(Modifier.width(8.dp));Button(onClick={appVm.sendMessage(body);body=""},enabled=body.isNotBlank()&&!state.actionBusy){Text("إرسال")}
            }
            TextButton(onClick=appVm::loadMessages){Text("تحديث الرسائل")}
        }
    }
}

@Composable private fun StoreTab(state:WanasUiState,appVm:WanasViewModel){
    var receiver by remember{mutableStateOf("")}
    var selectedGift by remember{mutableStateOf<Gift?>(null)}
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(8.dp))
        Text("المتجر والهدايا",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        Card(Modifier.fillMaxWidth().padding(top=10.dp),shape=RoundedCornerShape(18.dp)){ListItem(headlineContent={Text("رصيد العملات")},trailingContent={Text((state.wallet?.coins?:0).toString()+" 🪙")})}
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(receiver,{receiver=it},label={Text("معرّف المستلم لإرسال هدية")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        SectionTitle("الهدايا")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(state.gifts,key={it.id}){gift->
                Card{ListItem(
                    headlineContent={Text(gift.emoji+" "+gift.name)},
                    supportingContent={Text(gift.price_coins.toString()+" عملة")},
                    trailingContent={Button(onClick={selectedGift=gift},enabled=receiver.isNotBlank()){Text("اختيار")}}
                )}
            }
        }
        selectedGift?.let{g->Button(onClick={appVm.sendGift(receiver.trim(),g.id,1);selectedGift=null},modifier=Modifier.fillMaxWidth(),enabled=!state.actionBusy){Text("إرسال "+g.emoji+" "+g.name)}}
        SectionTitle("آخر العمليات")
        state.coinTransactions.take(8).forEach{tx->Text(tx.description+" • "+tx.amount+" • رصيد "+tx.balance_after,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=2.dp))}
    }
}

@Composable private fun ProfileTab(state:WanasUiState,appVm:WanasViewModel,signOut:()->Unit){
    var edit by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()){
        Spacer(Modifier.height(10.dp))
        Card(shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){
            Text(state.profile?.display_name?:"مستخدم وَنَس",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("@"+(state.profile?.username?:""),color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp));Text(state.profile?.bio?.ifBlank{"لا يوجد نبذة"}?:"")
        }}
        SectionTitle("الإحصائيات")
        state.stats?.let{StatsGrid(it)}
        if(state.achievements.isNotEmpty()){SectionTitle("الإنجازات");state.achievements.take(5).forEach{a->Text(a.icon+" "+a.title+" — "+a.description,modifier=Modifier.padding(vertical=3.dp))}}
        if(state.reports.isNotEmpty()){SectionTitle("بلاغاتك");state.reports.take(5).forEach{r->Text("• "+r.reason+" — "+r.status,modifier=Modifier.padding(vertical=3.dp))}}
        if(state.adminRole!=null){Spacer(Modifier.height(10.dp));Card{ListItem(headlineContent={Text("وضع الإدارة")},supportingContent={Text("الدور: "+state.adminRole.role)})}}
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick={edit=true},modifier=Modifier.fillMaxWidth()){Text("تعديل الملف الشخصي")}
        Spacer(Modifier.height(8.dp));OutlinedButton(onClick=signOut,modifier=Modifier.fillMaxWidth()){Text("تسجيل الخروج")}
    }
    if(edit) EditProfileDialog(state.profile?.display_name?:"",state.profile?.bio?:"",{edit=false}){name,bio->edit=false;appVm.updateProfile(name,bio)}
}

@Composable private fun RoomStage(state:RoomUiState,vm:WansRoomViewModel){
    var roomChat by remember { mutableStateOf("") }
    val context=LocalContext.current
    val micController = remember { MicController(context) }
    LaunchedEffect(state.micOn) { if (state.micOn) micController.setEnabled(true) else micController.setEnabled(false) }
    var micPermission by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)}
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->micPermission=granted;if(granted)vm.toggleMic()}
    Column(Modifier.fillMaxSize().padding(14.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Column{Text("الغرفة المباشرة",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(state.roomId.take(18)+" • "+state.members.size+" عضو")}
            TextButton(onClick={ micController.reset(); vm.leave() }){Text("مغادرة")}
        }
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(12.dp)){
            Text("المقاعد",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            for(row in 0 until 2){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                    for(col in 0 until 4){
                        val index=row*4+col
                        val member=state.members.firstOrNull{it.seat_index==index}
                        SeatItem(index,member){vm.takeSeat(index)}
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }}
        Spacer(Modifier.height(8.dp))
        Text("أحداث الغرفة",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        LazyColumn(Modifier.heightIn(min=80.dp,max=170.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
            items(state.events.takeLast(30),key={it.id}){e->
                val textValue = e.payload["text"]?.toString()?.trim('"') ?: when(e.event_type){
                    "reaction" -> "تفاعل: " + (e.payload["value"]?.toString()?.trim('"') ?: "")
                    "raised_hand" -> "✋ رفع اليد"
                    "pk" -> "⚔️ PK"
                    else -> e.event_type
                }
                Text(textValue,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=2.dp))
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            OutlinedTextField(roomChat,{roomChat=it},modifier=Modifier.weight(1f),singleLine=true,label={Text("اكتب في الغرفة")})
            Spacer(Modifier.width(5.dp))
            Button(onClick={vm.sendRoomChat(roomChat);roomChat=""},enabled=roomChat.isNotBlank()){Text("إرسال")}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            AssistChip(onClick={vm.sendReaction("❤️")},label={Text("❤️")})
            AssistChip(onClick={vm.sendReaction("👏")},label={Text("👏")})
            AssistChip(onClick={vm.sendReaction("🔥")},label={Text("🔥")})
            AssistChip(onClick=vm::raiseHand,label={Text("✋")})
            if(state.isOwner) AssistChip(onClick=vm::togglePkBattle,label={Text("PK")})
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
            items(state.members,key={it.room_id+":"+it.user_id}){m->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    ListItem(
                        modifier=Modifier.weight(1f),
                        headlineContent={Text(m.user_id.take(14))},
                        supportingContent={Text(m.role+" • "+if(m.is_microphone_on)"🎙 يتحدث" else "مستمع")},
                        trailingContent={if(m.is_muted)Text("🔇") else Text("")}
                    )
                    if(state.isOwner && m.user_id != vm.currentUserId()){
                        TextButton(onClick={vm.muteMember(m.user_id,!m.is_muted)}){Text(if(m.is_muted)"فتح" else "كتم")}
                        TextButton(onClick={vm.kickMember(m.user_id)}){Text("طرد")}
                    }
                }
            }
        }
        Button(
            modifier=Modifier.fillMaxWidth(),
            onClick={if(micPermission) { vm.toggleMic() } else { request.launch(Manifest.permission.RECORD_AUDIO) }},
            shape=RoundedCornerShape(16.dp)
        ){Text(if(state.micOn)"إيقاف المايك" else "تشغيل المايك")}
        if(state.isOwner){ Spacer(Modifier.height(6.dp)); OutlinedButton(onClick={vm::muteAll},modifier=Modifier.fillMaxWidth()){Text("كتم جميع المتحدثين")} }
        if(state.error!=null)Text(state.error,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(6.dp))
    }
}

@Composable private fun SeatItem(index:Int,member:RoomMember?,onClick:()->Unit){
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable(onClick=onClick).padding(3.dp)){
        Box(Modifier.size(48.dp).clip(CircleShape).background(if(member==null)MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text(if(member==null)"+" else "👤")}
        Text(if(member==null)"مقعد "+(index+1) else "متحدث",style=MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun RoomCard(room:VoiceRoom,onJoin:()->Unit){
    Card(Modifier.fillMaxWidth().clickable(onClick=onJoin),shape=RoundedCornerShape(18.dp)){
        Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),contentAlignment=Alignment.Center){Text("🎙")}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){Text(room.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text(room.category+" • "+room.max_members+" عضو",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Button(onClick=onJoin){Text("دخول")}
        }
    }
}
@Composable private fun ProfileActionCard(user:Profile,onFriend:()->Unit,onChat:()->Unit,onBuzz:()->Unit){
    Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(10.dp)){
        Text(user.display_name,fontWeight=FontWeight.Bold);Text("@"+user.username,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){TextButton(onClick=onFriend){Text("إضافة")};TextButton(onClick=onChat){Text("محادثة")};TextButton(onClick=onBuzz){Text("Buzz")}}
    }}
}
@Composable private fun NotificationCard(n:AppNotification,onRead:()->Unit){
    Card(Modifier.fillMaxWidth()){ListItem(headlineContent={Text(n.title)},supportingContent={Text(n.body)},trailingContent={if(!n.is_read)TextButton(onClick=onRead){Text("قرأت")}})}
}
@Composable private fun StatsGrid(s:UserStats){
    Column{Text("انتصارات "+s.wins+" • خسائر "+s.losses+" • تحديات "+s.total_challenges);Text("Buzz "+s.total_buzzes+" • ستريك "+s.current_streak+" • أفضل "+s.best_streak);Text("XP أسبوعي "+s.weekly_xp+" • شهري "+s.monthly_xp)}
}
@Composable private fun SectionTitle(t:String){Text(t,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))}
@Composable private fun EmptyCard(t:String){Card(Modifier.fillMaxWidth()){Box(Modifier.padding(22.dp),contentAlignment=Alignment.Center){Text(t,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
private fun friendLabel(f:Friendship,current:String?):String=if(f.requester_id==current)f.addressee_id.take(18) else f.requester_id.take(18)

@Composable private fun CreateRoomDialog(onDismiss:()->Unit,onCreate:(String,String)->Unit){
    var title by remember{mutableStateOf("")};var category by remember{mutableStateOf("عام")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("إنشاء غرفة")},text={Column{
        OutlinedTextField(title,{title=it},label={Text("اسم الغرفة")},singleLine=true)
        Spacer(Modifier.height(8.dp));OutlinedTextField(category,{category=it},label={Text("التصنيف")},singleLine=true)
    }},confirmButton={Button(onClick={onCreate(title,category)},enabled=title.isNotBlank()){Text("إنشاء")}},dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}})
}
@Composable private fun EditProfileDialog(oldName:String,oldBio:String,onDismiss:()->Unit,onSave:(String,String)->Unit){
    var name by remember{mutableStateOf(oldName)};var bio by remember{mutableStateOf(oldBio)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("تعديل الملف")},text={Column{
        OutlinedTextField(name,{name=it},label={Text("الاسم")});Spacer(Modifier.height(8.dp));OutlinedTextField(bio,{bio=it},label={Text("النبذة")})
    }},confirmButton={Button(onClick={onSave(name,bio)}){Text("حفظ")}},dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}})
}
