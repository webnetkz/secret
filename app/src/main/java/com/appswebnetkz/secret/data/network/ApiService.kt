package com.appswebnetkz.secret.data.network

import com.appswebnetkz.secret.data.model.ChatMessage
import com.appswebnetkz.secret.data.model.ChatRoom
import com.appswebnetkz.secret.data.model.CompleteProfileLinkSessionRequest
import com.appswebnetkz.secret.data.model.CreateChatRequest
import com.appswebnetkz.secret.data.model.CreateProfileLinkSessionRequest
import com.appswebnetkz.secret.data.model.ActivateAdminRequest
import com.appswebnetkz.secret.data.model.DeleteChatResponse
import com.appswebnetkz.secret.data.model.DeleteMessageResponse
import com.appswebnetkz.secret.data.model.HealthResponse
import com.appswebnetkz.secret.data.model.JoinChatRequest
import com.appswebnetkz.secret.data.model.LogoutRequest
import com.appswebnetkz.secret.data.model.ProfileLinkSessionResponse
import com.appswebnetkz.secret.data.model.RegisterUserRequest
import com.appswebnetkz.secret.data.model.SendMessageRequest
import com.appswebnetkz.secret.data.model.UpdateChatIconRequest
import com.appswebnetkz.secret.data.model.UpdateUserRequest
import com.appswebnetkz.secret.data.model.UploadResponse
import com.appswebnetkz.secret.data.model.UserProfile
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @POST("admin/activate")
    suspend fun activateAdmin(@Body body: ActivateAdminRequest): UserProfile

    @POST("users/register")
    suspend fun registerUser(@Body body: RegisterUserRequest): UserProfile

    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserProfile

    @PUT("users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body body: UpdateUserRequest
    ): UserProfile

    @POST("profile-link/sessions")
    suspend fun createProfileLinkSession(
        @Body body: CreateProfileLinkSessionRequest
    ): ProfileLinkSessionResponse

    @GET("profile-link/sessions/{sessionId}")
    suspend fun getProfileLinkSession(
        @Path("sessionId") sessionId: String
    ): ProfileLinkSessionResponse

    @POST("profile-link/sessions/{sessionId}/complete")
    suspend fun completeProfileLinkSession(
        @Path("sessionId") sessionId: String,
        @Body body: CompleteProfileLinkSessionRequest
    ): ProfileLinkSessionResponse

    @GET("chats")
    suspend fun getChats(@Query("userId") userId: String): List<ChatRoom>

    @POST("chats/create")
    suspend fun createChat(@Body body: CreateChatRequest): ChatRoom

    @POST("chats/join")
    suspend fun joinChat(@Body body: JoinChatRequest): ChatRoom

    @GET("chats/{chatId}/messages")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("userId") userId: String,
        @Query("includeDeleted") includeDeleted: Int? = null
    ): List<ChatMessage>

    @PUT("chats/{chatId}/icon")
    suspend fun updateChatIcon(
        @Path("chatId") chatId: String,
        @Body body: UpdateChatIconRequest
    ): ChatRoom

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(
        @Path("chatId") chatId: String,
        @Query("userId") userId: String
    ): DeleteChatResponse

    @POST("messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): ChatMessage

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: String,
        @Query("userId") userId: String
    ): DeleteMessageResponse

    @Multipart
    @POST("upload")
    suspend fun upload(@Part file: MultipartBody.Part): UploadResponse

    @GET("health")
    suspend fun health(): HealthResponse
}
