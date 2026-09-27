package com.example.bodycamai.command

import java.util.UUID

enum class CommandMessageType { GROUP, PRIVATE, TASK_UPDATE, MEDIA }
enum class CollaborationTaskStatus { OPEN, IN_PROGRESS, DONE, CANCELLED }

data class CommandMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val recipientId: String? = null,
    val type: CommandMessageType,
    val text: String = "",
    val attachmentId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CommandTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val assigneeId: String? = null,
    val pointId: String? = null,
    val status: CollaborationTaskStatus = CollaborationTaskStatus.OPEN,
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis()
)

object CommandCollaborationCodec {
    fun encodeMessage(message: CommandMessage): ByteArray = org.json.JSONObject().apply {
        put("id", message.id); put("senderId", message.senderId); put("recipientId", message.recipientId ?: org.json.JSONObject.NULL)
        put("type", message.type.name); put("text", message.text); put("attachmentId", message.attachmentId ?: org.json.JSONObject.NULL)
        put("timestamp", message.timestamp)
    }.toString().toByteArray(Charsets.UTF_8)

    fun encodeTask(task: CommandTask): ByteArray = org.json.JSONObject().apply {
        put("id", task.id); put("title", task.title); put("description", task.description)
        put("assigneeId", task.assigneeId ?: org.json.JSONObject.NULL); put("pointId", task.pointId ?: org.json.JSONObject.NULL)
        put("status", task.status.name); put("createdBy", task.createdBy); put("updatedAt", task.updatedAt)
    }.toString().toByteArray(Charsets.UTF_8)

    fun decodeMessage(bytes: ByteArray): CommandMessage {
        val o = org.json.JSONObject(String(bytes, Charsets.UTF_8))
        return CommandMessage(
            o.getString("id"), o.getString("senderId"), if (o.isNull("recipientId")) null else o.getString("recipientId"),
            CommandMessageType.valueOf(o.getString("type")), o.optString("text", ""),
            if (o.isNull("attachmentId")) null else o.getString("attachmentId"), o.getLong("timestamp")
        )
    }

    fun decodeTask(bytes: ByteArray): CommandTask {
        val o = org.json.JSONObject(String(bytes, Charsets.UTF_8))
        return CommandTask(
            o.getString("id"), o.getString("title"), o.optString("description", ""),
            if (o.isNull("assigneeId")) null else o.getString("assigneeId"),
            if (o.isNull("pointId")) null else o.getString("pointId"),
            CollaborationTaskStatus.valueOf(o.getString("status")), o.getString("createdBy"), o.getLong("updatedAt")
        )
    }
}

class CommandCollaborationStore {
    private val messages = linkedMapOf<String, CommandMessage>()
    private val tasks = linkedMapOf<String, CommandTask>()

    @Synchronized fun addMessage(message: CommandMessage) { messages.putIfAbsent(message.id, message) }
    @Synchronized fun messagesFor(peerId: String?): List<CommandMessage> =
        messages.values.filter { it.recipientId == null || it.recipientId == peerId }.sortedBy { it.timestamp }

    @Synchronized fun upsertTask(task: CommandTask) { tasks[task.id] = task }
    @Synchronized fun tasks(): List<CommandTask> = tasks.values.sortedByDescending { it.updatedAt }
}
