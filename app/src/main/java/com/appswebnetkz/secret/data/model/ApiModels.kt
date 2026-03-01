package com.appswebnetkz.secret.data.model

enum class MessageType {
    TEXT,
    FILE,
    AUDIO,
    VIDEO
}

data class UserProfile(
    val id: String,
    val nickname: String,
    val avatarUrl: String?,
    val createdAt: String,
    val isSuperAdmin: Boolean = false
)

data class ProfileLinkSessionResponse(
    val sessionId: String,
    val status: String,
    val source: String,
    val createdAt: String,
    val expiresAt: String,
    val completedAt: String?,
    val initiatorUserId: String?,
    val resolvedUserId: String?,
    val qrText: String,
    val user: UserProfile?
)

data class ChatRoom(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val hasPassword: Boolean,
    val createdBy: String,
    val createdAt: String,
    val membersCount: Int,
    val isDeleted: Boolean = false
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val userId: String,
    val senderNickname: String,
    val senderAvatarUrl: String?,
    val type: MessageType,
    val text: String?,
    val fileUrl: String?,
    val fileName: String?,
    val createdAt: String,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val deletedBy: String? = null
)

data class UploadResponse(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val messageType: MessageType
)

data class ApiError(
    val message: String
)

data class RegisterUserRequest(
    val nickname: String?,
    val avatarUrl: String?
)

data class LogoutRequest(
    val userId: String?
)

data class ActivateAdminRequest(
    val userId: String,
    val key: String
)

data class CreateProfileLinkSessionRequest(
    val userId: String?,
    val source: String?
)

data class CompleteProfileLinkSessionRequest(
    val userId: String?
)

data class UpdateUserRequest(
    val nickname: String?,
    val avatarUrl: String?
)

data class CreateChatRequest(
    val name: String,
    val password: String?,
    val iconUrl: String?,
    val userId: String
)

data class JoinChatRequest(
    val name: String,
    val password: String?,
    val userId: String
)

data class SendMessageRequest(
    val chatId: String,
    val userId: String,
    val type: MessageType,
    val text: String?,
    val fileUrl: String?,
    val fileName: String?
)

data class UpdateChatIconRequest(
    val userId: String,
    val iconUrl: String?
)

data class DeleteChatResponse(
    val chatId: String,
    val removedMessagesCount: Int,
    val isDeleted: Boolean = false,
    val chat: ChatRoom? = null
)

data class DeleteMessageResponse(
    val id: String,
    val chatId: String,
    val isDeleted: Boolean = false
)

data class HealthResponse(
    val status: String
)
