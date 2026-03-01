package com.appswebnetkz.secret.data.network

import com.appswebnetkz.secret.data.model.ChatMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class SocketClient(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private var listener: ((SocketEvent) -> Unit)? = null

    fun setMessageListener(onMessage: (SocketEvent) -> Unit) {
        listener = onMessage
    }

    fun connectIfNeeded() {
        if (webSocket != null) {
            return
        }

        val request = Request.Builder()
            .url(NetworkConfig.BASE_WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener?.invoke(SocketEvent.ConnectionChanged(isConnected = true))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull() ?: return
                val type = json.get("type")?.asString ?: return
                val payload = json.get("payload") ?: return

                when (type) {
                    "message" -> {
                        val message = runCatching { gson.fromJson(payload, ChatMessage::class.java) }.getOrNull() ?: return
                        listener?.invoke(SocketEvent.MessageReceived(message))
                    }

                    "message_deleted" -> {
                        val id = payload.asJsonObject?.get("id")?.asString ?: return
                        val chatId = payload.asJsonObject?.get("chatId")?.asString ?: return
                        listener?.invoke(SocketEvent.MessageDeleted(id = id, chatId = chatId))
                    }

                    "chat_deleted" -> {
                        val chatId = payload.asJsonObject?.get("chatId")?.asString ?: return
                        listener?.invoke(SocketEvent.ChatDeleted(chatId = chatId))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@SocketClient.webSocket = null
                listener?.invoke(SocketEvent.ConnectionChanged(isConnected = false))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@SocketClient.webSocket = null
                listener?.invoke(SocketEvent.ConnectionChanged(isConnected = false))
            }
        })
    }

    fun joinRoom(chatId: String, userId: String) {
        connectIfNeeded()
        val payload = gson.toJson(mapOf("type" to "join", "chatId" to chatId, "userId" to userId))
        webSocket?.send(payload)
    }

    fun leaveRoom(chatId: String) {
        val payload = gson.toJson(mapOf("type" to "leave", "chatId" to chatId))
        webSocket?.send(payload)
    }

    fun close() {
        webSocket?.close(1000, "closed")
        webSocket = null
        listener?.invoke(SocketEvent.ConnectionChanged(isConnected = false))
    }

    sealed interface SocketEvent {
        data class MessageReceived(val message: ChatMessage) : SocketEvent
        data class MessageDeleted(val id: String, val chatId: String) : SocketEvent
        data class ChatDeleted(val chatId: String) : SocketEvent
        data class ConnectionChanged(val isConnected: Boolean) : SocketEvent
    }
}
