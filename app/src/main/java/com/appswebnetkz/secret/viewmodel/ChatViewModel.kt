package com.appswebnetkz.secret.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appswebnetkz.secret.data.local.SessionStore
import com.appswebnetkz.secret.data.model.ChatMessage
import com.appswebnetkz.secret.data.model.ChatRoom
import com.appswebnetkz.secret.data.model.MessageType
import com.appswebnetkz.secret.data.model.UserProfile
import com.appswebnetkz.secret.data.network.NetworkModule
import com.appswebnetkz.secret.data.network.SocketClient
import com.appswebnetkz.secret.data.repository.ChatRepository
import com.appswebnetkz.secret.ui.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class LocaleMode {
    EN,
    RU
}

enum class ThemeMode {
    LIGHT,
    DARK
}

enum class ConnectionState {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

sealed interface AppScreen {
    data object Onboarding : AppScreen
    data object Home : AppScreen
    data object Profile : AppScreen
    data object Chat : AppScreen
    data object Settings : AppScreen
}

data class ChatUiState(
    val isLoading: Boolean = true,
    val user: UserProfile? = null,
    val chats: List<ChatRoom> = emptyList(),
    val activeChat: ChatRoom? = null,
    val messages: List<ChatMessage> = emptyList(),
    val unreadByChat: Map<String, Int> = emptyMap(),
    val locale: LocaleMode = LocaleMode.EN,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val socketConnected: Boolean = false,
    val backendHealthy: Boolean = false,
    val reconnecting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSavingProfile: Boolean = false,
    val isCreatingProfile: Boolean = false,
    val isLinkingProfile: Boolean = false,
    val isActivatingAdmin: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isUpdatingChatIcon: Boolean = false,
    val isSendingMessage: Boolean = false,
    val isJoiningChat: Boolean = false,
    val joinPasswordRequired: Boolean = false,
    val pendingJoinChatName: String? = null,
    val isGeneratingProfileQr: Boolean = false,
    val profileLinkQrText: String? = null
) {
    val connectionState: ConnectionState
        get() {
            if (socketConnected && backendHealthy) {
                return ConnectionState.CONNECTED
            }
            if (reconnecting) {
                return ConnectionState.RECONNECTING
            }
            return ConnectionState.DISCONNECTED
        }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(
        context = application,
        api = NetworkModule.apiService,
        sessionStore = SessionStore(application),
        socketClient = SocketClient(NetworkModule.okHttpClient, NetworkModule.gson),
        gson = NetworkModule.gson
    )

    private val joinedRoomIds = linkedSetOf<String>()
    private val clientSendTimestamps = mutableListOf<Long>()
    private val screenHistory = ArrayDeque<AppScreen>()
    private var healthJob: Job? = null

    var uiState by mutableStateOf(ChatUiState())
        private set

    var screen by mutableStateOf<AppScreen>(AppScreen.Onboarding)
        private set

    init {
        initialize()
    }

    private fun initialize() {
        NotificationHelper.ensureChannel(getApplication())

        val locale = if (repository.readLocaleCode().equals("ru", ignoreCase = true)) LocaleMode.RU else LocaleMode.EN
        val theme = if (repository.readThemeMode().equals("dark", ignoreCase = true)) ThemeMode.DARK else ThemeMode.LIGHT
        uiState = uiState.copy(
            locale = locale,
            theme = theme,
            unreadByChat = repository.readUnreadByChat(),
            isLoading = true,
            errorMessage = null,
            infoMessage = null
        )

        startHealthChecks()

        viewModelScope.launch {
            val existingUser = runCatching { repository.restoreUserFromSession() }.getOrNull()
            if (existingUser == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    user = null,
                    chats = emptyList(),
                    activeChat = null,
                    messages = emptyList(),
                    errorMessage = null
                )
                setScreen(AppScreen.Onboarding, addToHistory = false)
                return@launch
            }

            runCatching {
                bootstrapAuthenticatedUser(existingUser, autoOpenFirst = false)
            }.onFailure { error ->
                // Preserve existing local session if backend is temporarily unreachable.
                uiState = uiState.copy(
                    isLoading = false,
                    user = existingUser,
                    chats = emptyList(),
                    activeChat = null,
                    messages = emptyList(),
                    errorMessage = repository.getReadableError(error)
                )
                setScreen(AppScreen.Home, addToHistory = false)
            }
        }
    }

    private suspend fun bootstrapAuthenticatedUser(user: UserProfile, autoOpenFirst: Boolean) {
        repository.closeSocket()
        joinedRoomIds.clear()
        screenHistory.clear()
        repository.connectSocket(::onSocketEvent)

        val chats = applyStoredChatOrder(repository.getChats(user.id))
        persistChatOrder(chats)
        retainUnreadForVisibleChats(chats)

        uiState = uiState.copy(
            isLoading = false,
            user = user,
            chats = chats,
            activeChat = null,
            messages = emptyList(),
            isSavingProfile = false,
            isCreatingProfile = false,
            isLinkingProfile = false,
            isActivatingAdmin = false,
            isLoggingOut = false,
            isUpdatingChatIcon = false,
            isSendingMessage = false,
            isJoiningChat = false,
            joinPasswordRequired = false,
            pendingJoinChatName = null,
            isGeneratingProfileQr = false,
            profileLinkQrText = null,
            socketConnected = false,
            reconnecting = true,
            errorMessage = null,
            infoMessage = null
        )

        syncSocketRooms(chats)
        setScreen(AppScreen.Home, addToHistory = false)

        if (autoOpenFirst && chats.isNotEmpty()) {
            openChatInternal(chats.first(), showChatScreen = true)
        }
    }

    fun setLocale(locale: LocaleMode) {
        repository.writeLocaleCode(locale.name.lowercase())
        uiState = uiState.copy(locale = locale)
    }

    fun setTheme(theme: ThemeMode) {
        repository.writeThemeMode(theme.name.lowercase())
        uiState = uiState.copy(theme = theme)
    }

    fun openProfile() {
        if (uiState.user == null) {
            return
        }
        setScreen(AppScreen.Profile)
    }

    fun openSettings() {
        if (uiState.user == null) {
            return
        }
        setScreen(AppScreen.Settings)
    }

    fun openHome() {
        val target = if (uiState.user == null) AppScreen.Onboarding else AppScreen.Home
        setScreen(target)
    }

    fun goBack() {
        var previous: AppScreen? = null
        while (screenHistory.isNotEmpty() && previous == null) {
            val candidate = screenHistory.removeLast()
            val isForbiddenOnboarding = candidate is AppScreen.Onboarding && uiState.user != null
            if (!isForbiddenOnboarding) {
                previous = candidate
            }
        }
        if (previous != null) {
            setScreen(previous, addToHistory = false)
            return
        }

        when (screen) {
            AppScreen.Chat -> closeChat()
            AppScreen.Profile, AppScreen.Settings -> openHome()
            else -> Unit
        }
    }

    fun createNewProfile() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isCreatingProfile = true,
                isLinkingProfile = false,
                isActivatingAdmin = false,
                errorMessage = null
            )
            runCatching {
                repository.createNewUser()
            }.onSuccess { createdUser ->
                bootstrapAuthenticatedUser(createdUser, autoOpenFirst = true)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isCreatingProfile = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun linkProfileFromQrPayload(rawPayload: String) {
        val sessionId = repository.extractProfileLinkSessionId(rawPayload)
        if (sessionId.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "Invalid QR code")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(
                isLinkingProfile = true,
                isCreatingProfile = false,
                isActivatingAdmin = false,
                errorMessage = null
            )
            runCatching {
                repository.completeProfileLinkSession(
                    sessionId = sessionId,
                    userId = uiState.user?.id
                )
            }.onSuccess { session ->
                val linkedUser = session.user
                if (session.status == "EXPIRED") {
                    uiState = uiState.copy(
                        isLinkingProfile = false,
                        errorMessage = "QR expired. Generate a new one in web client."
                    )
                    return@onSuccess
                }
                if (linkedUser == null) {
                    uiState = uiState.copy(
                        isLinkingProfile = false,
                        errorMessage = "Unable to resolve linked profile"
                    )
                    return@onSuccess
                }

                bootstrapAuthenticatedUser(linkedUser, autoOpenFirst = true)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isLinkingProfile = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun generateProfileLinkQr() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isGeneratingProfileQr = true,
                errorMessage = null
            )
            runCatching {
                repository.createProfileLinkSession(userId = uiState.user?.id)
            }.onSuccess { session ->
                uiState = uiState.copy(
                    isGeneratingProfileQr = false,
                    profileLinkQrText = session.qrText
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    isGeneratingProfileQr = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun clearProfileLinkQr() {
        if (uiState.profileLinkQrText == null) {
            return
        }
        uiState = uiState.copy(profileLinkQrText = null)
    }

    fun refreshChats(autoOpenFirst: Boolean = true) {
        val user = uiState.user ?: return
        viewModelScope.launch {
            runCatching { repository.getChats(user.id) }
                .onSuccess { loaded ->
                    val ordered = applyStoredChatOrder(loaded)
                    persistChatOrder(ordered)
                    retainUnreadForVisibleChats(ordered)

                    val activeId = uiState.activeChat?.id
                    val matchedActive = ordered.firstOrNull { it.id == activeId }

                    uiState = uiState.copy(
                        chats = ordered,
                        activeChat = matchedActive,
                        messages = if (matchedActive != null) uiState.messages else emptyList(),
                        errorMessage = null
                    )

                    syncSocketRooms(ordered)

                    if (autoOpenFirst && ordered.isNotEmpty()) {
                        val first = ordered.first()
                        if (uiState.activeChat?.id != first.id) {
                            viewModelScope.launch {
                                openChatInternal(first, showChatScreen = true)
                            }
                        }
                    }
                }
                .onFailure { error ->
                    uiState = uiState.copy(errorMessage = repository.getReadableError(error))
                }
        }
    }

    fun createChat(name: String, password: String?, iconUri: Uri?) {
        val user = uiState.user ?: return
        val chatName = name.trim()
        if (chatName.length < 4) {
            uiState = uiState.copy(errorMessage = localized("chat_name_min"))
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.createChat(user.id, chatName, password, iconUri)
            }.onSuccess { chat ->
                refreshChats(autoOpenFirst = false)
                openChat(chat)
                uiState = uiState.copy(infoMessage = localized("chat_created"))
            }.onFailure { error ->
                uiState = uiState.copy(errorMessage = repository.getReadableError(error))
            }
        }
    }

    fun joinChat(name: String, password: String?) {
        val user = uiState.user ?: return
        val chatName = name.trim()
        if (chatName.isBlank()) {
            uiState = uiState.copy(errorMessage = localized("enter_chat_name"))
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isJoiningChat = true, errorMessage = null)
            runCatching {
                repository.joinChat(user.id, chatName, password)
            }.onSuccess { chat ->
                uiState = uiState.copy(
                    isJoiningChat = false,
                    joinPasswordRequired = false,
                    pendingJoinChatName = null
                )
                refreshChats(autoOpenFirst = false)
                openChat(chat)
                uiState = uiState.copy(infoMessage = localized("chat_joined"))
            }.onFailure { error ->
                val unauthorized = error is HttpException && error.code() == 401
                if (unauthorized) {
                    uiState = uiState.copy(
                        isJoiningChat = false,
                        joinPasswordRequired = true,
                        pendingJoinChatName = chatName,
                        errorMessage = if (password.isNullOrBlank()) {
                            localized("need_password")
                        } else {
                            localized("wrong_password")
                        }
                    )
                    return@onFailure
                }

                uiState = uiState.copy(
                    isJoiningChat = false,
                    joinPasswordRequired = false,
                    pendingJoinChatName = null,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun onJoinChatNameChanged() {
        if (!uiState.joinPasswordRequired && uiState.pendingJoinChatName == null) {
            return
        }
        uiState = uiState.copy(
            joinPasswordRequired = false,
            pendingJoinChatName = null
        )
    }

    fun openChat(chat: ChatRoom) {
        viewModelScope.launch {
            openChatInternal(chat, showChatScreen = true)
        }
    }

    private suspend fun openChatInternal(chat: ChatRoom, showChatScreen: Boolean) {
        val user = uiState.user ?: return
        uiState = uiState.copy(
            isLoading = true,
            activeChat = chat,
            errorMessage = null
        )
        if (showChatScreen) {
            setScreen(AppScreen.Chat)
        }

        runCatching {
            val includeDeleted = canViewDeleted(chat)
            repository.getMessages(chat.id, user.id, includeDeleted = includeDeleted)
        }.onSuccess { loadedMessages ->
            val includeDeleted = canViewDeleted(chat)
            val sanitized = loadedMessages
                .filter { message -> includeDeleted || !message.isDeleted }
                .let(::deduplicateMessages)

            clearUnread(chat.id)
            uiState = uiState.copy(
                isLoading = false,
                activeChat = chat,
                messages = sanitized,
                errorMessage = null
            )
        }.onFailure { error ->
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = repository.getReadableError(error)
            )
        }
    }

    fun closeChat() {
        uiState = uiState.copy(activeChat = null, messages = emptyList())
        setScreen(AppScreen.Home)
    }

    fun saveProfile(nickname: String, avatarUri: Uri?) {
        val user = uiState.user ?: return
        viewModelScope.launch {
            uiState = uiState.copy(isSavingProfile = true, errorMessage = null)
            runCatching {
                repository.updateProfile(user.id, nickname, avatarUri)
            }.onSuccess { updatedUser ->
                uiState = uiState.copy(user = updatedUser, isSavingProfile = false, errorMessage = null)
                setScreen(AppScreen.Home)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isSavingProfile = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun activateAdmin(adminKey: String) {
        val user = uiState.user ?: return
        val key = adminKey.trim()
        if (key.isBlank()) {
            uiState = uiState.copy(errorMessage = localized("admin_key_required"))
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(
                isActivatingAdmin = true,
                errorMessage = null
            )
            runCatching {
                repository.activateAdmin(userId = user.id, key = key)
            }.onSuccess { updated ->
                uiState = uiState.copy(
                    user = updated,
                    isActivatingAdmin = false,
                    infoMessage = localized("admin_activated")
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    isActivatingAdmin = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun logout() {
        val userId = uiState.user?.id
        viewModelScope.launch {
            uiState = uiState.copy(
                isLoggingOut = true,
                errorMessage = null
            )
            runCatching {
                repository.logout(userId)
            }.onSuccess {
                joinedRoomIds.clear()
                screenHistory.clear()
                uiState = ChatUiState(
                    isLoading = false,
                    locale = uiState.locale,
                    theme = uiState.theme
                )
                setScreen(AppScreen.Onboarding, addToHistory = false)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isLoggingOut = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun updateActiveChatIcon(iconUri: Uri?) {
        val user = uiState.user ?: return
        val chat = uiState.activeChat ?: return
        if (!canManageChat(chat)) {
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isUpdatingChatIcon = true, errorMessage = null)
            runCatching {
                repository.updateChatIcon(chat.id, user.id, iconUri)
            }.onSuccess { updatedChat ->
                applyUpdatedChat(updatedChat)
                uiState = uiState.copy(isUpdatingChatIcon = false)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isUpdatingChatIcon = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun deleteChat(chat: ChatRoom) {
        val user = uiState.user ?: return
        if (!canManageChat(chat)) {
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.deleteChat(chat.id, user.id)
            }.onSuccess { response ->
                val shouldKeepDeleted = canViewDeleted(chat)
                clearUnread(chat.id)

                if (shouldKeepDeleted) {
                    val deletedChat = response.chat?.copy(isDeleted = true) ?: chat.copy(isDeleted = true)
                    applyUpdatedChat(deletedChat)
                    if (uiState.activeChat?.id == chat.id) {
                        uiState = uiState.copy(activeChat = deletedChat)
                    }
                } else {
                    val remaining = uiState.chats.filter { it.id != chat.id }
                    uiState = uiState.copy(
                        chats = remaining,
                        activeChat = if (uiState.activeChat?.id == chat.id) null else uiState.activeChat,
                        messages = if (uiState.activeChat?.id == chat.id) emptyList() else uiState.messages
                    )
                    persistChatOrder(remaining)
                    syncSocketRooms(remaining)

                    if (uiState.activeChat == null && remaining.isNotEmpty()) {
                        viewModelScope.launch {
                            openChatInternal(remaining.first(), showChatScreen = true)
                        }
                    }
                }

                uiState = uiState.copy(infoMessage = localized("chat_deleted"))
            }.onFailure { error ->
                uiState = uiState.copy(errorMessage = repository.getReadableError(error))
            }
        }
    }

    fun moveChat(chatId: String, direction: Int) {
        if (direction == 0 || uiState.chats.isEmpty()) {
            return
        }

        val current = uiState.chats.toMutableList()
        val sourceIndex = current.indexOfFirst { it.id == chatId }
        if (sourceIndex < 0) {
            return
        }

        val targetIndex = (sourceIndex + direction).coerceIn(0, current.lastIndex)
        if (sourceIndex == targetIndex) {
            return
        }

        val item = current.removeAt(sourceIndex)
        current.add(targetIndex, item)
        uiState = uiState.copy(chats = current)
        persistChatOrder(current)

        uiState.activeChat?.id?.let { activeId ->
            val updatedActive = current.firstOrNull { it.id == activeId }
            if (updatedActive != null) {
                uiState = uiState.copy(activeChat = updatedActive)
            }
        }
    }

    fun sendText(text: String) {
        val chat = uiState.activeChat ?: return
        val user = uiState.user ?: return
        val payload = text.trim()
        if (payload.isBlank()) {
            return
        }
        if (chat.isDeleted) {
            uiState = uiState.copy(errorMessage = localized("chat_deleted_readonly"))
            return
        }
        if (!reserveClientMessageSlot()) {
            uiState = uiState.copy(errorMessage = localized("rate_limit"))
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSendingMessage = true)
            runCatching {
                repository.sendText(chat.id, user.id, payload)
            }.onSuccess { message ->
                appendMessageIfMissing(message)
                uiState = uiState.copy(isSendingMessage = false, errorMessage = null)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isSendingMessage = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun sendMedia(uri: Uri, forcedType: MessageType? = null) {
        val chat = uiState.activeChat ?: return
        val user = uiState.user ?: return
        if (chat.isDeleted) {
            uiState = uiState.copy(errorMessage = localized("chat_deleted_readonly"))
            return
        }
        if (!reserveClientMessageSlot()) {
            uiState = uiState.copy(errorMessage = localized("rate_limit"))
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSendingMessage = true)
            runCatching {
                repository.sendMedia(chat.id, user.id, uri, forcedType)
            }.onSuccess { message ->
                appendMessageIfMissing(message)
                uiState = uiState.copy(isSendingMessage = false, errorMessage = null)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isSendingMessage = false,
                    errorMessage = repository.getReadableError(error)
                )
            }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val user = uiState.user ?: return
        if (!canDeleteMessage(message)) {
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.deleteMessage(message.id, user.id)
            }.onSuccess {
                if (canViewDeleted(uiState.activeChat)) {
                    markMessageDeleted(message.id)
                } else {
                    removeMessage(message.id)
                }
            }.onFailure { error ->
                uiState = uiState.copy(errorMessage = repository.getReadableError(error))
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun clearInfo() {
        uiState = uiState.copy(infoMessage = null)
    }

    fun unreadCount(chatId: String): Int = uiState.unreadByChat[chatId] ?: 0

    private fun onSocketEvent(event: SocketClient.SocketEvent) {
        when (event) {
            is SocketClient.SocketEvent.ConnectionChanged -> {
                uiState = uiState.copy(
                    socketConnected = event.isConnected,
                    reconnecting = !event.isConnected || !uiState.backendHealthy
                )
                if (event.isConnected) {
                    syncSocketRooms(uiState.chats)
                }
            }

            is SocketClient.SocketEvent.MessageReceived -> {
                val message = event.message
                if (message.chatId == uiState.activeChat?.id) {
                    if (!message.isDeleted || canViewDeleted(uiState.activeChat)) {
                        appendMessageIfMissing(message)
                    }
                    clearUnread(message.chatId)
                } else if (message.userId != uiState.user?.id) {
                    incrementUnread(message.chatId)
                    val chatName = uiState.chats.firstOrNull { it.id == message.chatId }?.name ?: "Secret chat"
                    NotificationHelper.showIncomingMessage(
                        context = getApplication(),
                        chatName = chatName,
                        sender = message.senderNickname,
                        body = previewMessageText(message)
                    )
                }
            }

            is SocketClient.SocketEvent.MessageDeleted -> {
                if (event.chatId != uiState.activeChat?.id) {
                    return
                }
                if (canViewDeleted(uiState.activeChat)) {
                    markMessageDeleted(event.id)
                } else {
                    removeMessage(event.id)
                }
            }

            is SocketClient.SocketEvent.ChatDeleted -> {
                val existing = uiState.chats.firstOrNull { it.id == event.chatId } ?: return
                if (canViewDeleted(existing)) {
                    val updated = existing.copy(isDeleted = true)
                    applyUpdatedChat(updated)
                    uiState = uiState.copy(infoMessage = localized("chat_deleted"))
                } else {
                    val remaining = uiState.chats.filter { it.id != event.chatId }
                    val activeWasDeleted = uiState.activeChat?.id == event.chatId
                    clearUnread(event.chatId)
                    uiState = uiState.copy(
                        chats = remaining,
                        activeChat = if (activeWasDeleted) null else uiState.activeChat,
                        messages = if (activeWasDeleted) emptyList() else uiState.messages,
                        infoMessage = localized("chat_deleted")
                    )
                    persistChatOrder(remaining)
                    syncSocketRooms(remaining)
                    if (activeWasDeleted && remaining.isNotEmpty()) {
                        viewModelScope.launch {
                            openChatInternal(remaining.first(), showChatScreen = true)
                        }
                    }
                }
            }
        }
    }

    private fun canManageChat(chat: ChatRoom): Boolean {
        val user = uiState.user ?: return false
        return chat.createdBy == user.id || user.isSuperAdmin
    }

    private fun canViewDeleted(chat: ChatRoom?): Boolean {
        val currentUser = uiState.user ?: return false
        val targetChat = chat ?: return false
        return targetChat.createdBy == currentUser.id || currentUser.isSuperAdmin
    }

    private fun canDeleteMessage(message: ChatMessage): Boolean {
        val user = uiState.user ?: return false
        if (message.isDeleted) {
            return false
        }
        return message.userId == user.id || user.isSuperAdmin
    }

    private fun applyUpdatedChat(updated: ChatRoom) {
        val next = uiState.chats.map { if (it.id == updated.id) updated else it }
        uiState = uiState.copy(
            chats = next,
            activeChat = if (uiState.activeChat?.id == updated.id) updated else uiState.activeChat
        )
        persistChatOrder(next)
    }

    private fun appendMessageIfMissing(message: ChatMessage) {
        val incomingId = message.id
        val incomingFingerprint = messageFingerprint(message)
        val exists = uiState.messages.any { existing ->
            (incomingId.isNotBlank() && existing.id == incomingId) || messageFingerprint(existing) == incomingFingerprint
        }
        if (exists) {
            return
        }

        uiState = uiState.copy(messages = uiState.messages + message)
    }

    private fun removeMessage(messageId: String) {
        uiState = uiState.copy(messages = uiState.messages.filterNot { it.id == messageId })
    }

    private fun markMessageDeleted(messageId: String) {
        uiState = uiState.copy(
            messages = uiState.messages.map { message ->
                if (message.id != messageId) {
                    message
                } else {
                    message.copy(isDeleted = true)
                }
            }
        )
    }

    private fun deduplicateMessages(items: List<ChatMessage>): List<ChatMessage> {
        val seen = hashSetOf<String>()
        val unique = mutableListOf<ChatMessage>()
        items.forEach { item ->
            val key = if (item.id.isNotBlank()) item.id else messageFingerprint(item)
            if (seen.add(key)) {
                unique += item
            }
        }
        return unique
    }

    private fun messageFingerprint(message: ChatMessage): String {
        return listOf(
            message.chatId,
            message.userId,
            message.type.name,
            message.text.orEmpty(),
            message.fileUrl.orEmpty(),
            message.fileName.orEmpty(),
            message.createdAt
        ).joinToString(separator = "|")
    }

    private fun previewMessageText(message: ChatMessage): String {
        if (!message.text.isNullOrBlank()) {
            return message.text
        }
        return when (message.type) {
            MessageType.AUDIO -> if (uiState.locale == LocaleMode.RU) "Аудиосообщение" else "Audio message"
            MessageType.VIDEO -> if (uiState.locale == LocaleMode.RU) "Видеосообщение" else "Video message"
            MessageType.FILE -> message.fileName ?: if (uiState.locale == LocaleMode.RU) "Файл" else "File"
            MessageType.TEXT -> if (uiState.locale == LocaleMode.RU) "Новое сообщение" else "New message"
        }
    }

    private fun reserveClientMessageSlot(): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - CLIENT_MESSAGE_WINDOW_MS
        clientSendTimestamps.removeAll { it <= windowStart }
        if (clientSendTimestamps.size >= CLIENT_MESSAGE_LIMIT_PER_SECOND) {
            return false
        }
        clientSendTimestamps += now
        return true
    }

    private fun applyStoredChatOrder(chatList: List<ChatRoom>): List<ChatRoom> {
        val order = repository.readChatOrder()
        if (order.isEmpty()) {
            return chatList
        }

        val rank = order.withIndex().associate { (index, id) -> id to index }
        return chatList
            .withIndex()
            .sortedWith(compareBy({ rank[it.value.id] ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
    }

    private fun persistChatOrder(chatList: List<ChatRoom>) {
        repository.writeChatOrder(chatList.map { it.id })
    }

    private fun retainUnreadForVisibleChats(chatList: List<ChatRoom>) {
        val allowed = chatList.map { it.id }.toHashSet()
        val next = uiState.unreadByChat
            .filterKeys { allowed.contains(it) }
            .filterValues { it > 0 }
        uiState = uiState.copy(unreadByChat = next)
        repository.writeUnreadByChat(next)
    }

    private fun incrementUnread(chatId: String) {
        val nextCount = (uiState.unreadByChat[chatId] ?: 0) + 1
        val next = uiState.unreadByChat.toMutableMap().apply {
            put(chatId, nextCount)
        }
        uiState = uiState.copy(unreadByChat = next)
        repository.writeUnreadByChat(next)
    }

    private fun clearUnread(chatId: String) {
        if (!uiState.unreadByChat.containsKey(chatId)) {
            return
        }
        val next = uiState.unreadByChat.toMutableMap().apply {
            remove(chatId)
        }
        uiState = uiState.copy(unreadByChat = next)
        repository.writeUnreadByChat(next)
    }

    private fun syncSocketRooms(chats: List<ChatRoom>) {
        val userId = uiState.user?.id ?: return

        val target = chats.map { it.id }.toSet()
        target.forEach { chatId ->
            if (!joinedRoomIds.contains(chatId)) {
                repository.joinSocketRoom(chatId, userId)
                joinedRoomIds.add(chatId)
            }
        }

        val removed = joinedRoomIds.filterNot { target.contains(it) }
        removed.forEach { chatId ->
            repository.leaveSocketRoom(chatId)
            joinedRoomIds.remove(chatId)
        }
    }

    private fun startHealthChecks() {
        healthJob?.cancel()
        healthJob = viewModelScope.launch {
            while (isActive) {
                val healthy = repository.isBackendHealthy()
                uiState = uiState.copy(
                    backendHealthy = healthy,
                    reconnecting = !healthy || !uiState.socketConnected
                )

                if (healthy && uiState.user != null && !uiState.socketConnected) {
                    repository.connectSocket(::onSocketEvent)
                    syncSocketRooms(uiState.chats)
                }

                delay(4000)
            }
        }
    }

    private fun localized(key: String): String {
        val ru = uiState.locale == LocaleMode.RU
        return when (key) {
            "chat_name_min" -> if (ru) "Название чата должно быть от 4 символов" else "Chat name must be at least 4 characters"
            "enter_chat_name" -> if (ru) "Введите название чата" else "Enter chat name"
            "need_password" -> if (ru) "Чат найден. Введите пароль" else "Chat found. Enter password"
            "wrong_password" -> if (ru) "Неверный пароль" else "Wrong password"
            "chat_created" -> if (ru) "Чат создан" else "Chat created"
            "chat_joined" -> if (ru) "Подключено к чату" else "Connected to chat"
            "chat_deleted" -> if (ru) "Чат удален" else "Chat deleted"
            "chat_deleted_readonly" -> if (ru) "Чат удален. Только просмотр" else "Chat deleted. Read-only"
            "rate_limit" -> if (ru) "Слишком много сообщений. Не более 5 в секунду" else "Too many messages. Max 5 per second"
            "admin_key_required" -> if (ru) "Введите ключ администратора" else "Enter admin key"
            "admin_activated" -> if (ru) "Режим суперадмина активирован" else "Super admin mode activated"
            else -> key
        }
    }

    override fun onCleared() {
        healthJob?.cancel()
        repository.closeSocket()
        super.onCleared()
    }

    private fun setScreen(target: AppScreen, addToHistory: Boolean = true) {
        val safeTarget = if (target is AppScreen.Onboarding && uiState.user != null) {
            AppScreen.Home
        } else {
            target
        }
        if (screen == safeTarget) {
            return
        }
        if (addToHistory) {
            screenHistory.addLast(screen)
            while (screenHistory.size > 24) {
                screenHistory.removeFirst()
            }
        }
        screen = safeTarget
    }

    companion object {
        private const val CLIENT_MESSAGE_LIMIT_PER_SECOND = 5
        private const val CLIENT_MESSAGE_WINDOW_MS = 1_000L
    }
}
