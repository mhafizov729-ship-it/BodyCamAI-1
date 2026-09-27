package com.example.bodycamai.network

import com.example.bodycamai.core.BackendConfig
import com.example.bodycamai.core.GroupMessage
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

interface BackendTransport {
    suspend fun sendMessage(config: BackendConfig, message: GroupMessage): Result<Unit>
}

/** Generic HTTPS transport. The app does not ship with a private server or credentials. */
class HttpBackendTransport : BackendTransport {
    override suspend fun sendMessage(config: BackendConfig, message: GroupMessage): Result<Unit> = runCatching {
        require(config.serverUrl.startsWith("https://")) { "Для удалённого сервера требуется HTTPS" }
        require(config.authenticated) { "Устройство не авторизовано" }
        val connection = (URL(config.serverUrl.trimEnd('/') + "/v1/groups/${config.groupId}/messages").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Device-Id", config.deviceId)
        }
        val body = JSONObject().apply {
            put("id", message.id)
            put("senderId", message.senderId)
            put("senderName", message.senderName)
            put("text", message.text)
            put("type", message.type.name)
            put("timestamp", message.timestamp)
        }.toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299) error("Сервер вернул HTTP $code")
    }
}
