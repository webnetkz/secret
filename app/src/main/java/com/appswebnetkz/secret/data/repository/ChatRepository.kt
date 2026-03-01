package com.appswebnetkz.secret.data.repository

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.appswebnetkz.secret.data.local.SessionStore
import com.appswebnetkz.secret.data.model.ActivateAdminRequest
import com.appswebnetkz.secret.data.model.ApiError
import com.appswebnetkz.secret.data.model.ChatMessage
import com.appswebnetkz.secret.data.model.ChatRoom
import com.appswebnetkz.secret.data.model.CompleteProfileLinkSessionRequest
import com.appswebnetkz.secret.data.model.CreateChatRequest
import com.appswebnetkz.secret.data.model.CreateProfileLinkSessionRequest
import com.appswebnetkz.secret.data.model.JoinChatRequest
import com.appswebnetkz.secret.data.model.LogoutRequest
import com.appswebnetkz.secret.data.model.MessageType
import com.appswebnetkz.secret.data.model.ProfileLinkSessionResponse
import com.appswebnetkz.secret.data.model.RegisterUserRequest
import com.appswebnetkz.secret.data.model.SendMessageRequest
import com.appswebnetkz.secret.data.model.UpdateChatIconRequest
import com.appswebnetkz.secret.data.model.UpdateUserRequest
import com.appswebnetkz.secret.data.model.UserProfile
import com.appswebnetkz.secret.data.network.ApiService
import com.appswebnetkz.secret.data.network.SocketClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.lang.reflect.Type
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val api: ApiService,
    private val sessionStore: SessionStore,
    private val socketClient: SocketClient,
    private val gson: Gson
) {
    suspend fun restoreUserFromSession(): UserProfile? = withContext(Dispatchers.IO) {
        val savedUserId = sessionStore.userId
        if (savedUserId.isNullOrBlank()) {
            return@withContext null
        }

        val remoteResult = runCatching { api.getUser(savedUserId) }
        val existing = remoteResult.getOrNull()
        if (existing != null) {
            saveSession(existing)
            return@withContext existing
        }

        val failure = remoteResult.exceptionOrNull()
        if (failure is HttpException && failure.code() == 404) {
            // The profile no longer exists on backend, reset local session.
            sessionStore.clearUserSession()
            return@withContext null
        }

        // Keep local session on transient backend/network errors to avoid forced re-registration.
        return@withContext UserProfile(
            id = savedUserId,
            nickname = sessionStore.nickname?.takeIf { it.isNotBlank() } ?: defaultGuestNickname(),
            avatarUrl = sessionStore.avatarUrl,
            createdAt = "",
            isSuperAdmin = false
        )
    }

    suspend fun createNewUser(): UserProfile = withContext(Dispatchers.IO) {
        val generatedNickname = sessionStore.nickname?.takeIf { it.isNotBlank() } ?: defaultGuestNickname()
        val newUser = api.registerUser(
            RegisterUserRequest(
                nickname = generatedNickname,
                avatarUrl = sessionStore.avatarUrl
            )
        )
        saveSession(newUser)
        return@withContext newUser
    }

    suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarUri: Uri?
    ): UserProfile = withContext(Dispatchers.IO) {
        val avatarUrl = if (avatarUri != null) {
            uploadUri(avatarUri, forRoundAvatar = true).first
        } else {
            sessionStore.avatarUrl
        }

        val response = api.updateUser(
            userId = userId,
            body = UpdateUserRequest(
                nickname = nickname.ifBlank { null },
                avatarUrl = avatarUrl
            )
        )

        saveSession(response)
        return@withContext response
    }

    suspend fun getChats(userId: String): List<ChatRoom> = withContext(Dispatchers.IO) {
        return@withContext api.getChats(userId)
    }

    suspend fun createChat(userId: String, name: String, password: String?, iconUri: Uri? = null): ChatRoom = withContext(Dispatchers.IO) {
        val iconUrl = iconUri?.let { uploadUri(it, forRoundAvatar = true).first }
        return@withContext api.createChat(
            CreateChatRequest(
                name = name,
                password = password?.ifBlank { null },
                iconUrl = iconUrl,
                userId = userId
            )
        )
    }

    suspend fun joinChat(userId: String, name: String, password: String?): ChatRoom = withContext(Dispatchers.IO) {
        return@withContext api.joinChat(
            JoinChatRequest(
                name = name,
                password = password?.ifBlank { null },
                userId = userId
            )
        )
    }

    suspend fun getMessages(chatId: String, userId: String, includeDeleted: Boolean = false): List<ChatMessage> = withContext(Dispatchers.IO) {
        val includeDeletedFlag = if (includeDeleted) 1 else null
        return@withContext api.getMessages(
            chatId = chatId,
            userId = userId,
            includeDeleted = includeDeletedFlag
        )
    }

    suspend fun sendText(chatId: String, userId: String, text: String): ChatMessage = withContext(Dispatchers.IO) {
        return@withContext api.sendMessage(
            SendMessageRequest(
                chatId = chatId,
                userId = userId,
                type = MessageType.TEXT,
                text = text,
                fileUrl = null,
                fileName = null
            )
        )
    }

    suspend fun sendMedia(
        chatId: String,
        userId: String,
        uri: Uri,
        forcedType: MessageType? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        val (url, uploadedName, serverType) = uploadUri(uri, forcedType)
        return@withContext api.sendMessage(
            SendMessageRequest(
                chatId = chatId,
                userId = userId,
                type = forcedType ?: serverType,
                text = null,
                fileUrl = url,
                fileName = uploadedName
            )
        )
    }

    suspend fun updateChatIcon(chatId: String, userId: String, iconUri: Uri?): ChatRoom = withContext(Dispatchers.IO) {
        val iconUrl = iconUri?.let { uploadUri(it, forRoundAvatar = true).first }
        return@withContext api.updateChatIcon(
            chatId = chatId,
            body = UpdateChatIconRequest(
                userId = userId,
                iconUrl = iconUrl
            )
        )
    }

    suspend fun deleteChat(chatId: String, userId: String) = withContext(Dispatchers.IO) {
        return@withContext api.deleteChat(chatId = chatId, userId = userId)
    }

    suspend fun deleteMessage(messageId: String, userId: String) = withContext(Dispatchers.IO) {
        return@withContext api.deleteMessage(messageId = messageId, userId = userId)
    }

    suspend fun isBackendHealthy(): Boolean = withContext(Dispatchers.IO) {
        return@withContext runCatching { api.health().status.equals("ok", ignoreCase = true) }.getOrDefault(false)
    }

    suspend fun activateAdmin(userId: String, key: String): UserProfile = withContext(Dispatchers.IO) {
        val updated = api.activateAdmin(
            ActivateAdminRequest(
                userId = userId,
                key = key
            )
        )
        saveSession(updated)
        return@withContext updated
    }

    suspend fun logout(userId: String?) = withContext(Dispatchers.IO) {
        runCatching {
            api.logout(LogoutRequest(userId = userId?.takeIf { it.isNotBlank() }))
        }
        closeSocket()
        sessionStore.clearUserSession()
        sessionStore.unreadByChatJson = "{}"
        sessionStore.chatOrderJson = "[]"
    }

    suspend fun createProfileLinkSession(userId: String?): ProfileLinkSessionResponse = withContext(Dispatchers.IO) {
        return@withContext api.createProfileLinkSession(
            CreateProfileLinkSessionRequest(
                userId = userId?.takeIf { it.isNotBlank() },
                source = "ANDROID"
            )
        )
    }

    suspend fun completeProfileLinkSession(sessionId: String, userId: String?): ProfileLinkSessionResponse = withContext(Dispatchers.IO) {
        val response = api.completeProfileLinkSession(
            sessionId = sessionId,
            body = CompleteProfileLinkSessionRequest(userId = userId?.takeIf { it.isNotBlank() })
        )

        val linkedUser = response.user
            ?: response.resolvedUserId?.let { resolvedId ->
                runCatching { api.getUser(resolvedId) }.getOrNull()
            }

        if (linkedUser != null) {
            saveSession(linkedUser)
        }

        return@withContext if (linkedUser != null && response.user == null) {
            response.copy(user = linkedUser)
        } else {
            response
        }
    }

    fun extractProfileLinkSessionId(rawPayload: String): String? {
        val value = rawPayload.trim()
        if (value.isBlank()) {
            return null
        }

        if (UUID_REGEX.matches(value)) {
            return value
        }

        val parsed = runCatching { Uri.parse(value) }.getOrNull()
        parsed?.getQueryParameter("session")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.trim() }

        val markerIndex = value.indexOf("session=", ignoreCase = true)
        if (markerIndex >= 0) {
            val raw = value.substring(markerIndex + "session=".length)
            val extracted = raw.substringBefore("&").substringBefore("#").trim()
            if (extracted.isNotBlank()) {
                return extracted
            }
        }

        return null
    }

    fun connectSocket(onMessage: (SocketClient.SocketEvent) -> Unit) {
        socketClient.setMessageListener(onMessage)
        socketClient.connectIfNeeded()
    }

    fun joinSocketRoom(chatId: String, userId: String) {
        socketClient.joinRoom(chatId, userId)
    }

    fun leaveSocketRoom(chatId: String) {
        socketClient.leaveRoom(chatId)
    }

    fun closeSocket() {
        socketClient.close()
    }

    fun currentUserId(): String? = sessionStore.userId

    fun readLocaleCode(): String = sessionStore.localeCode

    fun writeLocaleCode(code: String) {
        sessionStore.localeCode = code
    }

    fun readThemeMode(): String = sessionStore.themeMode

    fun writeThemeMode(mode: String) {
        sessionStore.themeMode = mode
    }

    fun readChatOrder(): List<String> {
        val raw = sessionStore.chatOrderJson
        val type: Type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        val parsed = runCatching { gson.fromJson<List<String>>(raw, type) }.getOrNull() ?: emptyList()
        return parsed.filter { it.isNotBlank() }
    }

    fun writeChatOrder(order: List<String>) {
        sessionStore.chatOrderJson = gson.toJson(order)
    }

    fun readUnreadByChat(): Map<String, Int> {
        val raw = sessionStore.unreadByChatJson
        val type: Type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val parsed = runCatching { gson.fromJson<Map<String, Int>>(raw, type) }.getOrNull() ?: emptyMap()
        return parsed
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, count) -> count.coerceAtLeast(0) }
            .filterValues { it > 0 }
    }

    fun writeUnreadByChat(unreadByChat: Map<String, Int>) {
        sessionStore.unreadByChatJson = gson.toJson(unreadByChat)
    }

    fun getReadableError(error: Throwable): String {
        if (error is IllegalArgumentException && !error.message.isNullOrBlank()) {
            return error.message ?: "Validation error"
        }
        if (error is HttpException) {
            val rawBody = error.response()?.errorBody()?.string()
            if (!rawBody.isNullOrBlank()) {
                val parsed = runCatching { gson.fromJson(rawBody, ApiError::class.java) }.getOrNull()
                if (!parsed?.message.isNullOrBlank()) {
                    return parsed?.message ?: "Server error"
                }
            }
            return "HTTP ${error.code()}"
        }
        return error.message ?: "Unknown error"
    }

    private fun saveSession(user: UserProfile) {
        sessionStore.saveUserSession(
            userId = user.id,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl
        )
    }

    private fun defaultGuestNickname(): String {
        return "guest-${UUID.randomUUID().toString().take(6)}"
    }

    private suspend fun uploadUri(
        uri: Uri,
        forcedType: MessageType? = null,
        forRoundAvatar: Boolean = false
    ): Triple<String, String, MessageType> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var displayName = queryDisplayName(uri)
            ?: guessDisplayNameFromUri(uri)
            ?: "file-${System.currentTimeMillis()}"
        var mimeType = resolver.getType(uri) ?: guessMimeTypeByName(displayName) ?: "application/octet-stream"
        val reportedSize = queryFileSize(uri)
        if (reportedSize != null && reportedSize > MAX_UPLOAD_BYTES) {
            throw IllegalArgumentException("File is too large. Maximum size is 50 MB")
        }

        if (forcedType == MessageType.AUDIO && mimeType == "application/octet-stream") {
            mimeType = "audio/mp4"
            if (!displayName.lowercase().endsWith(".m4a")) {
                displayName = "$displayName.m4a"
            }
        } else if (forcedType == MessageType.VIDEO && mimeType == "application/octet-stream") {
            mimeType = "video/mp4"
            if (!displayName.lowercase().endsWith(".mp4")) {
                displayName = "$displayName.mp4"
            }
        }

        val safeDisplayName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val tempFile = File(context.cacheDir, "upload-${UUID.randomUUID()}-$safeDisplayName")

        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open file" }
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (tempFile.length() > MAX_UPLOAD_BYTES) {
            tempFile.delete()
            throw IllegalArgumentException("File is too large. Maximum size is 50 MB")
        }

        try {
            val preparedFile = if (mimeType.startsWith("image/")) {
                if (forRoundAvatar) {
                    prepareRoundAvatarImage(tempFile)
                } else {
                    compressImage(tempFile)
                }
            } else {
                tempFile
            }
            val uploadMimeType = if (preparedFile.absolutePath != tempFile.absolutePath) "image/jpeg" else mimeType
            val uploadDisplayName = if (preparedFile.absolutePath != tempFile.absolutePath) {
                val nameWithoutExt = displayName.substringBeforeLast(".", displayName)
                "$nameWithoutExt.jpg"
            } else {
                displayName
            }

            if (preparedFile.length() > MAX_UPLOAD_BYTES) {
                preparedFile.delete()
                if (preparedFile.absolutePath != tempFile.absolutePath) {
                    tempFile.delete()
                }
                throw IllegalArgumentException("File is too large. Maximum size is 50 MB")
            }

            if (forcedType == MessageType.AUDIO || mimeType.startsWith("audio/")) {
                val durationSec = readAudioDurationSec(preparedFile)
                if (durationSec > MAX_AUDIO_DURATION_SEC) {
                    preparedFile.delete()
                    if (preparedFile.absolutePath != tempFile.absolutePath) {
                        tempFile.delete()
                    }
                    throw IllegalArgumentException("Audio is too long. Maximum length is 1000 seconds")
                }
            }

            val requestBody = preparedFile.asRequestBody(uploadMimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", uploadDisplayName, requestBody)
            val response = api.upload(part)
            if (preparedFile.absolutePath != tempFile.absolutePath) {
                preparedFile.delete()
            }
            return@withContext Triple(response.url, response.fileName, response.messageType)
        } finally {
            tempFile.delete()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)
                } else {
                    null
                }
            } else {
                null
            }
        } finally {
            cursor?.close()
        }
    }

    private fun queryFileSize(uri: Uri): Long? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) {
                    cursor.getLong(index)
                } else {
                    null
                }
            } else {
                null
            }
        } finally {
            cursor?.close()
        }
    }

    private fun guessDisplayNameFromUri(uri: Uri): String? {
        val segment = uri.lastPathSegment?.substringAfterLast('/')?.trim().orEmpty()
        return segment.takeIf { it.isNotBlank() }
    }

    private fun guessMimeTypeByName(name: String?): String? {
        val lower = name?.lowercase()?.trim().orEmpty()
        if (lower.isBlank()) {
            return null
        }
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".heic") || lower.endsWith(".heif") -> "image/heic"
            lower.endsWith(".mp4") || lower.endsWith(".m4v") -> "video/mp4"
            lower.endsWith(".mov") -> "video/quicktime"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".m4a") -> "audio/mp4"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".aac") -> "audio/aac"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ogg") || lower.endsWith(".oga") || lower.endsWith(".opus") -> "audio/ogg"
            else -> null
        }
    }

    private fun compressImage(sourceFile: File): File {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return sourceFile
        }

        var inSampleSize = 1
        var longestSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (longestSide > IMAGE_MAX_LONG_SIDE_PX) {
            inSampleSize *= 2
            longestSide /= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts) ?: return sourceFile
        val compressedFile = File(context.cacheDir, "upload-img-${UUID.randomUUID()}.jpg")

        try {
            var quality = 88
            var compressed: ByteArray
            do {
                val output = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, output)
                compressed = output.toByteArray()
                quality -= 8
            } while (compressed.size > MAX_UPLOAD_BYTES && quality >= 40)

            if (compressed.size > MAX_UPLOAD_BYTES) {
                compressedFile.delete()
                throw IllegalArgumentException("File is too large. Maximum size is 50 MB")
            }

            FileOutputStream(compressedFile).use { stream ->
                stream.write(compressed)
                stream.flush()
            }
            return compressedFile
        } finally {
            bitmap.recycle()
        }
    }

    private fun prepareRoundAvatarImage(sourceFile: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return sourceFile
        }

        var inSampleSize = 1
        var longestSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (longestSide > IMAGE_MAX_LONG_SIDE_PX) {
            inSampleSize *= 2
            longestSide /= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts) ?: return sourceFile
        val side = minOf(bitmap.width, bitmap.height)
        if (side <= 0) {
            bitmap.recycle()
            return sourceFile
        }

        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        val square = android.graphics.Bitmap.createBitmap(bitmap, left, top, side, side)
        val outputFile = File(context.cacheDir, "upload-avatar-${UUID.randomUUID()}.jpg")

        try {
            val output = ByteArrayOutputStream()
            square.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
            val compressed = output.toByteArray()
            if (compressed.size > MAX_UPLOAD_BYTES) {
                outputFile.delete()
                throw IllegalArgumentException("File is too large. Maximum size is 50 MB")
            }

            FileOutputStream(outputFile).use { stream ->
                stream.write(compressed)
                stream.flush()
            }
            return outputFile
        } finally {
            square.recycle()
            bitmap.recycle()
        }
    }

    private fun readAudioDurationSec(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            (rawDuration?.toLongOrNull() ?: 0L).div(1000L).toInt()
        } catch (_error: Exception) {
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private const val MAX_UPLOAD_BYTES = 50L * 1024L * 1024L
        private const val MAX_AUDIO_DURATION_SEC = 1_000
        private const val IMAGE_MAX_LONG_SIDE_PX = 1920
    }
}
