package com.aistudio.wanas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WanasUiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val wallet: Wallet? = null,
    val stats: UserStats? = null,
    val friends: List<Friendship> = emptyList(),
    val users: List<Profile> = emptyList(),
    val gifts: List<Gift> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val coinTransactions: List<CoinTransaction> = emptyList(),
    val reports: List<Report> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
    val actionBusy: Boolean = false,
    val adminRole: AdminRole? = null,
    val challenges: List<Challenge> = emptyList(),
    val dailyQuests: List<DailyQuest> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

class WanasViewModel : ViewModel() {
    private val repository = WanasRepository()
    private val challengeRepository = ChallengeRepository()
    private val _state = MutableStateFlow(WanasUiState())
    val state: StateFlow<WanasUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init { loadAll() }

    fun currentUserId(): String? = repository.currentUserId()

    fun loadAll() {
        viewModelScope.launch { runCatching { repository.setOnline(true) } }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val profile = repository.profile()
                val wallet = repository.wallet()
                val stats = repository.stats()
                val friends = repository.friendships()
                val gifts = repository.gifts()
                val notifications = repository.notifications()
                val coins = repository.coinTransactions()
                val admin = repository.adminRole()
                val reports = repository.reports()
                val challenges = challengeRepository.myChallenges()
                val quests = challengeRepository.dailyQuests()
                val achievements = challengeRepository.achievements()
                listOf(profile, wallet, stats, friends, gifts, notifications, coins, admin, challenges, quests, achievements, reports)
            }.onSuccess {
                _state.value = _state.value.copy(
                    loading = false,
                    profile = it[0] as Profile?,
                    wallet = it[1] as Wallet?,
                    stats = it[2] as UserStats?,
                    friends = @Suppress("UNCHECKED_CAST") (it[3] as List<Friendship>),
                    gifts = @Suppress("UNCHECKED_CAST") (it[4] as List<Gift>),
                    notifications = @Suppress("UNCHECKED_CAST") (it[5] as List<AppNotification>),
                    coinTransactions = @Suppress("UNCHECKED_CAST") (it[6] as List<CoinTransaction>),
                    adminRole = it[7] as AdminRole?,
                    challenges = @Suppress("UNCHECKED_CAST") (it[8] as List<Challenge>),
                    dailyQuests = @Suppress("UNCHECKED_CAST") (it[9] as List<DailyQuest>),
                    achievements = @Suppress("UNCHECKED_CAST") (it[10] as List<Achievement>),
                    reports = @Suppress("UNCHECKED_CAST") (it[11] as List<Report>)
                )
            }.onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "تعذر تحميل البيانات") }
            while (isActive) {
                delay(15_000)
                runCatching {
                    repository.profile()
                    repository.wallet()
                    repository.notifications()
                    repository.friendships()
                    _state.value = _state.value.copy(
                        profile = repository.profile(),
                        wallet = repository.wallet(),
                        friends = repository.friendships(),
                        notifications = repository.notifications()
                    )
                }.onFailure { e ->
                    _state.value = _state.value.copy(error = e.message ?: "تعذر تحديث البيانات")
                }
            }
        }
    }

    private fun loadLight() {
        viewModelScope.launch {
            runCatching {
                val p = repository.profile()
                val w = repository.wallet()
                val f = repository.friendships()
                val n = repository.notifications()
                _state.value = _state.value.copy(profile = p, wallet = w, friends = f, notifications = n)
            }
        }
    }

    fun searchUser(username: String) {
        viewModelScope.launch {
            runCatching { repository.usersByUsername(username.trim()) }
                .onSuccess { _state.value = _state.value.copy(users = it, error = null) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر البحث") }
        }
    }

    fun createChallenge(opponentId: String, title: String = "تحدي وَنَس") = action {
        challengeRepository.createChallenge(opponentId,title)
        _state.value = _state.value.copy(challenges = challengeRepository.myChallenges())
    }

    fun sendFriendRequest(userId: String) = action {
        repository.sendFriendRequest(userId)
        loadLight()
    }

    fun blockFriend(id: String) = action {
        repository.blockFriend(id)
        loadLight()
    }

    fun acceptFriendRequest(id: String) = action {
        repository.acceptFriendRequest(id)
        loadLight()
    }

    fun openConversation(userId: String) {
        action {
            _state.value = _state.value.copy(activeConversationId = repository.createConversation(userId))
            loadMessages()
        }
    }

    fun loadMessages() {
        val id = _state.value.activeConversationId ?: return
        viewModelScope.launch {
            runCatching { repository.messages(id) }
                .onSuccess { _state.value = _state.value.copy(messages = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "تعذر تحميل الرسائل") }
        }
    }

    fun sendMessage(body: String) {
        val id = _state.value.activeConversationId ?: return
        if (body.isBlank()) return
        action {
            repository.sendMessage(id, body)
            repository.markConversationRead(id)
            loadMessages()
        }
    }

    fun markNotificationRead(id: String) = action {
        repository.markNotificationRead(id)
        _state.value = _state.value.copy(notifications = _state.value.notifications.map {
            if (it.id == id) it.copy(is_read = true) else it
        })
    }

    fun sendGift(receiverId: String, giftId: String, quantity: Int = 1) = action {
        repository.sendGift(receiverId, giftId, quantity)
        loadLight()
    }

    fun claimDailyCoins() = action {
        repository.claimDailyCoins(100)
        _state.value = _state.value.copy(wallet = repository.wallet(), coinTransactions = repository.coinTransactions())
    }

    fun sendBuzz(receiverId: String) = action {
        repository.sendBuzz(receiverId)
    }

    fun updateProfile(displayName: String, bio: String) = action {
        val p = repository.updateProfile(displayName, bio)
        _state.value = _state.value.copy(profile = p)
    }

    fun reportUser(targetUserId: String?, reason: String, details: String) = action {
        repository.createReport(targetUserId, reason, details)
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionBusy = true, error = null)
            runCatching { block() }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message ?: "تعذر تنفيذ العملية")
            }
            _state.value = _state.value.copy(actionBusy = false)
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
