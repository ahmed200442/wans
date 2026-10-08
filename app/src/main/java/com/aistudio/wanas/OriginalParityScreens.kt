package com.aistudio.wanas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val WansGold = Color(0xFFFFB82E)
private val WansPurple = Color(0xFF2B2058)
private val WansTeal = Color(0xFF10BFA5)
private val WansBlue = Color(0xFF3978D8)

@Composable
fun OriginalParityHome(roomState: RoomUiState, roomVm: WansRoomViewModel, state: WanasUiState, appVm: WanasViewModel) {
    var selectedGoal by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
        item { ParityHero() }
        item { QuickAccountCard(state.profile, state.wallet, { appVm.loadAll() }) }
        item { MatchingCard(selectedGoal) { selectedGoal = it } }
        item { SectionHeader("الشاشة الرئيسية — اختار عايز تبدأ إزاي؟") }
        item { ActionGrid({ selectedGoal = "محتاج أتكلم" }, { selectedGoal = "عايز أسمع" }, { selectedGoal = "أصدقاء" }, { roomVm.refreshRooms() }) }
        item { LiveLobbyCard(roomState) { roomVm.refreshRooms() } }
        item { SectionHeader("طلبات أشخاص محتاجين يتكلموا الآن 💡") }
        item { EmptyParityCard(if (roomState.rooms.isEmpty()) "لا توجد طلبات متاحة حاليًا — افتح غرفة أو ادخل الصالة العامة." else "اختر غرفة مباشرة من الصالة وابدأ جلسة صوتية فورًا.") }
        item { SectionHeader("آخر الإشعارات") }
        items(state.notifications.take(4), key = { it.id }) { n -> NotificationCard(n) { appVm.markNotificationRead(n.id) } }
    }
}

@Composable private fun ParityHero() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(18.dp), color = WansGold) {
                Text("✨ أهلًا ومرحبًا بك في وَنَس — الناس للناس ⭐", Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = Color(0xFF21143A), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp)); Text("🎙️", style = MaterialTheme.typography.displayMedium)
            Text("نورّت بيتك الدافي للصحة الطيبة والفضفضة الصادقة 🌙", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("في وَنَس، الكلمة الحلوة بتجمعنا والقلوب بتسمع بعض.. اتكلم براحتك، اسمع من قلبك، وشارك أجمل السهرات الصوتية مع أصدقاء حقيقيين في أمان وخصوصية تامة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ParityPill("🎙 جلسات 10 دقائق", Modifier.weight(1f)); ParityPill("🌙 غرف وسهرات", Modifier.weight(1f)); ParityPill("🤝 أعضاء حقيقيون", Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun QuickAccountCard(profile: Profile?, wallet: Wallet?, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile?.display_name ?: "عضو وَنَس", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("VIP 💎  •  Level " + (profile?.level ?: 1), color = WansGold, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF17112F)) {
                    Text("Coin " + (wallet?.coins ?: 0) + " 🪙", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = WansGold, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WansGold)) { Text("⚡ دخول فوري", color = Color(0xFF21143A)) }
                OutlinedButton(onClick = onRefresh, Modifier.weight(1f)) { Text("☀ نهاري") }
            }
        }
    }
}

@Composable private fun MatchingCard(selected: String?, onSelect: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF211A45))) {
        Column(Modifier.padding(16.dp)) {
            Text("⭐ نظام المطابقة الذكي — «إنت داخل تعمل إيه؟»", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("بدل الاختيار العشوائي، اختار هدفك ونجهز لك التجربة المناسبة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            val goals = listOf("😔 محتاج أفضفض", "🗣️ كلام عادي ودردشة", "👂 عايز أسمع", "😂 دردشة وضحك", "🎮 ألعاب وتحديات", "🤝 تعارف وصداقات", "📚 مذاكرة وتركيز", "💼 نقاش عن العمل")
            goals.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { goal ->
                        val active = selected == goal
                        Button(onClick = { onSelect(goal) }, Modifier.weight(1f).padding(vertical = 3.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) WansGold else Color(0xFF17112F), contentColor = if (active) Color(0xFF21143A) else Color.White)) { Text(goal) }
                    }
                }
            }
            selected?.let { Spacer(Modifier.height(6.dp)); Text("الاختيار الحالي: " + it, color = WansTeal, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun ActionGrid(onTalk: () -> Unit, onListen: () -> Unit, onFriends: () -> Unit, onRooms: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParityAction("🗣️", "محتاج أتكلم", "يبحث فورًا عن شخص أختار «عايز أسمع»", WansBlue, onTalk)
            ParityAction("👂", "عايز أسمع", "شوف طلبات المتحدثين واختر شخصًا تسمعه", Color(0xFF0F8D8B), onListen)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParityAction("👥", "أصدقاء", "رسائل، Buzz، حالة الاتصال ومكالمات", WansBlue, onFriends)
            ParityAction("🎙️", "غرف صوتية", "سهرة مصرية، ضحك، كورة، ألعاب ومذاكرة", Color(0xFFC54A0A), onRooms)
        }
    }
}

@Composable private fun RowScope.ParityAction(emoji: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.displaySmall); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.White.copy(alpha = .85f))
        }
    }
}

@Composable private fun LiveLobbyCard(roomState: RoomUiState, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) {
        Column(Modifier.padding(16.dp)) {
            Text("🎙️ صالة الغرف الصوتية المباشرة (Lobby)", style = MaterialTheme.typography.titleLarge, color = WansGold, fontWeight = FontWeight.Bold)
            Text("استكشف الغرف المفتوحة الآن أو أنشئ غرفتك الخاصة بضغطة واحدة.")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(roomState.rooms.size.toString(), "غرف نشطة", Modifier.weight(1f)); StatBox(roomState.rooms.sumOf { it.max_members }.toString(), "إجمالي السعة", Modifier.weight(1f)); StatBox("8", "مايكات/غرفة", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpen, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WansGold)) { Text("🎙 فتح الصالة العامة", color = Color(0xFF21143A), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable fun OriginalParityRooms(state: RoomUiState, vm: WansRoomViewModel) {
    var category by remember { mutableStateOf("الكل") }; var showCreate by remember { mutableStateOf(false) }
    val categories = listOf("الكل", "سهرة مصرية", "دردشة وضحك", "فضفضة", "تعارف", "كورة", "ألعاب", "مذاكرة", "أغاني")
    val filtered = if (category == "الكل") state.rooms else state.rooms.filter { it.category == category }
    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) {
            Column(Modifier.padding(16.dp)) {
                Text("🎙️ صالة الغرف الصوتية المباشرة (Lobby)", style = MaterialTheme.typography.headlineSmall, color = WansGold, fontWeight = FontWeight.Bold)
                Text("استكشف الغرف المفتوحة الآن أو أنشئ غرفتك الخاصة بضغطة واحدة.")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(state.rooms.size.toString(), "غرفة عامة نشطة", Modifier.weight(1f)); StatBox(state.rooms.sumOf { it.max_members }.toString(), "السعة", Modifier.weight(1f)); StatBox("8", "مقاعد صوتية", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::refreshRooms, Modifier.weight(1f)) { Text("🌐 الغرف العامة") }
                    Button(onClick = { showCreate = true }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WansGold)) { Text("➕ إنشاء غرفة", color = Color(0xFF21143A)) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { categories.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) } } }
            if (filtered.isEmpty()) item { EmptyParityCard("🎙 لا توجد غرفة بهذا التصنيف حاليًا — أنشئ أول غرفة وابدأ السهرة.") }
            else items(filtered, key = { it.id }) { room -> RoomCard(room) { vm.join(room.id, null) } }
        }
    }
    if (showCreate) CreateRoomDialog({ showCreate = false }) { title, cat -> showCreate = false; vm.createRoom(title, cat) }
}

@Composable fun OriginalParityStore(state: WanasUiState, appVm: WanasViewModel) {
    var receiver by remember { mutableStateOf("") }; var quantity by remember { mutableStateOf(1) }; var selectedGift by remember { mutableStateOf<Gift?>(null) }
    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) {
            Column(Modifier.padding(18.dp)) {
                Text("🪙 Coins & الهدايا", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text((state.wallet?.coins ?: 0).toString() + " Coins", style = MaterialTheme.typography.displaySmall, color = WansGold, fontWeight = FontWeight.Black)
                Button(onClick = appVm::claimDailyCoins, enabled = !state.actionBusy, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WansGold)) { Text("🎁 استلام المكافأة اليومية +100 Coins", color = Color(0xFF21143A)) }
            }
        }
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = receiver, onValueChange = { receiver = it }, modifier = Modifier.fillMaxWidth(), label = { Text("اسم/معرّف المستلم") }, singleLine = true)
        Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1,5,10,99).forEach { q -> FilterChip(selected = quantity == q, onClick = { quantity = q }, label = { Text("x" + q) }) } }
        SectionTitle("🎁 صندوق الهدايا الافتراضية المتحركة")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.gifts, key = { it.id }) { gift ->
            Card(Modifier.fillMaxWidth().clickable { selectedGift = gift }, shape = RoundedCornerShape(18.dp)) { ListItem(headlineContent = { Text(gift.emoji + " " + gift.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text(gift.price_coins.toString() + " Coins • Combo x" + quantity) }, trailingContent = { Button(onClick = { selectedGift = gift }, enabled = receiver.isNotBlank()) { Text("إرسال") } }) }
        } }
        selectedGift?.let { gift -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF203D46))) { Column(Modifier.padding(12.dp)) { Text("🎁 " + gift.name + " → " + receiver); Text("Combo x" + quantity + " • " + (gift.price_coins * quantity) + " Coins"); Button(onClick = { appVm.sendGift(receiver.trim(), gift.id, quantity); selectedGift = null }, enabled = receiver.isNotBlank() && !state.actionBusy) { Text("إرسال الهدية فورًا 🔥") } } } }
        SectionTitle("آخر معاملات Coins"); state.coinTransactions.take(6).forEach { Text("• " + it.description + " — " + it.amount + " — الرصيد " + it.balance_after) }
    }
}

@Composable fun OriginalParityProfile(state: WanasUiState, appVm: WanasViewModel, signOut: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = WansPurple)) { Column(Modifier.padding(18.dp)) {
            Text("👑 " + (state.profile?.display_name ?: "عضو وَنَس"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("@" + (state.profile?.username ?: "")); Text("VIP 💎  •  Level " + (state.profile?.level ?: 1), color = WansGold, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(state.profile?.bio?.ifBlank { "أهلاً بك في وَنَس" } ?: "")
        } }
        SectionTitle("📊 إحصائياتك"); state.stats?.let { StatsGrid(it) }; SectionTitle("🏆 الإنجازات")
        if (state.achievements.isEmpty()) EmptyParityCard("لا توجد إنجازات مكتسبة بعد.") else state.achievements.take(8).forEach { Text(it.icon + " " + it.title + " — " + it.description, Modifier.padding(vertical = 3.dp)) }
        Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = signOut, Modifier.fillMaxWidth()) { Text("تسجيل الخروج") }
    }
}

@Composable private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) { Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17112F))) { Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = WansTeal, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun ParityPill(text: String, modifier: Modifier = Modifier) { Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color(0xFF17112F)) { Text(text, Modifier.padding(7.dp), style = MaterialTheme.typography.labelSmall) } }
@Composable private fun EmptyParityCard(text: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF211A45))) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionHeader(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }