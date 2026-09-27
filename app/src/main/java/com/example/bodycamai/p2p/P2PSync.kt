package com.example.bodycamai.p2p

import com.example.bodycamai.core.ConnectionChannel
import com.example.bodycamai.core.GroupMessage
import com.example.bodycamai.core.MessageType
import com.example.bodycamai.command.CommandMessage
import com.example.bodycamai.command.CommandTask
import com.example.bodycamai.command.CommandCollaborationCodec
import com.example.bodycamai.command.CommandCollaborationStore
import com.example.bodycamai.map.CommandMapPacket
import com.example.bodycamai.map.CommandMapCodec
import com.example.bodycamai.map.CommandMapStore
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/** Application packets carried over the encrypted serverless P2P session. */
sealed class SyncPacket {
    data class Message(val item: GroupMessage) : SyncPacket()
    data class Location(val deviceId: String, val latitude: Double, val longitude: Double, val accuracy: Float?, val timestamp: Long) : SyncPacket()
    data class Presence(val deviceId: String, val name: String, val online: Boolean, val batteryPercent: Int, val channel: ConnectionChannel) : SyncPacket()
    data class Ping(val timestamp: Long) : SyncPacket()
    data class CommandMessagePacket(val item: CommandMessage) : SyncPacket()
    data class CommandTaskPacket(val item: CommandTask) : SyncPacket()
    data class CommandMapPacketMessage(val item: CommandMapPacket) : SyncPacket()
}

object SyncCodec {
    fun encode(packet: SyncPacket): ByteArray {
        val o = JSONObject()
        when (packet) {
            is SyncPacket.Message -> {
                o.put("type", "message").put("id", packet.item.id).put("senderId", packet.item.senderId)
                    .put("senderName", packet.item.senderName).put("text", packet.item.text)
                    .put("messageType", packet.item.type.name).put("timestamp", packet.item.timestamp)
                    .put("channel", packet.item.channel.name)
            }
            is SyncPacket.Location -> o.put("type", "location").put("deviceId", packet.deviceId)
                .put("lat", packet.latitude).put("lon", packet.longitude)
                .put("accuracy", packet.accuracy ?: JSONObject.NULL).put("timestamp", packet.timestamp)
            is SyncPacket.Presence -> o.put("type", "presence").put("deviceId", packet.deviceId)
                .put("name", packet.name).put("online", packet.online)
                .put("battery", packet.batteryPercent).put("channel", packet.channel.name)
            is SyncPacket.Ping -> o.put("type", "ping").put("timestamp", packet.timestamp)
            is SyncPacket.CommandMessagePacket -> o.put("type", "command_message").put("payload", String(CommandCollaborationCodec.encodeMessage(packet.item), Charsets.UTF_8))
            is SyncPacket.CommandTaskPacket -> o.put("type", "command_task").put("payload", String(CommandCollaborationCodec.encodeTask(packet.item), Charsets.UTF_8))
            is SyncPacket.CommandMapPacketMessage -> o.put("type", "command_map").put("payload", String(CommandMapCodec.encode(packet.item), Charsets.UTF_8))
        }
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): SyncPacket {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        return when (o.getString("type")) {
            "message" -> SyncPacket.Message(GroupMessage(
                o.getString("id"), o.getString("senderId"), o.getString("senderName"), o.getString("text"),
                MessageType.valueOf(o.optString("messageType", "TEXT")), o.optLong("timestamp"),
                ConnectionChannel.valueOf(o.optString("channel", "OFFLINE"))
            ))
            "location" -> SyncPacket.Location(o.getString("deviceId"), o.getDouble("lat"), o.getDouble("lon"),
                if (o.isNull("accuracy")) null else o.optDouble("accuracy").toFloat(), o.optLong("timestamp"))
            "presence" -> SyncPacket.Presence(o.getString("deviceId"), o.getString("name"), o.optBoolean("online"),
                o.optInt("battery", 0), ConnectionChannel.valueOf(o.optString("channel", "OFFLINE")))
            "ping" -> SyncPacket.Ping(o.optLong("timestamp"))
            "command_message" -> SyncPacket.CommandMessagePacket(CommandCollaborationCodec.decodeMessage(o.getString("payload").toByteArray(Charsets.UTF_8)))
            "command_task" -> SyncPacket.CommandTaskPacket(CommandCollaborationCodec.decodeTask(o.getString("payload").toByteArray(Charsets.UTF_8)))
            "command_map" -> SyncPacket.CommandMapPacketMessage(CommandMapCodec.decode(o.getString("payload").toByteArray(Charsets.UTF_8)))
            else -> error("Неизвестный тип P2P-пакета")
        }
    }
}

data class PeerSnapshot(val deviceId: String, val name: String, val online: Boolean, val batteryPercent: Int, val channel: ConnectionChannel,
                        val latitude: Double? = null, val longitude: Double? = null, val lastSeen: Long = System.currentTimeMillis())

/** Keeps the synchronized group state in memory on the Host and Client. */
class P2PSyncEngine(
    private val localDeviceId: String,
    private val localName: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val peers = CopyOnWriteArrayList<PeerSnapshot>()
    private val listeners = CopyOnWriteArrayList<(SyncPacket) -> Unit>()
    val collaborationStore = CommandCollaborationStore()
    val commandMapStore = CommandMapStore()
    private var receiveJob: Job? = null

    fun addListener(listener: (SyncPacket) -> Unit) { listeners += listener }
    fun removeListener(listener: (SyncPacket) -> Unit) { listeners -= listener }
    fun snapshots(): List<PeerSnapshot> = peers.toList()

    fun attach(session: SecurePeerSession) {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            while (true) {
                val packet = runCatching { SyncCodec.decode(session.receive()) }.getOrElse { break }
                apply(packet)
                listeners.forEach { it(packet) }
            }
        }
    }

    fun send(session: SecurePeerSession, packet: SyncPacket): Result<Unit> = runCatching { session.send(SyncCodec.encode(packet)) }

    fun newText(text: String, channel: ConnectionChannel): SyncPacket.Message = SyncPacket.Message(
        GroupMessage(UUID.randomUUID().toString(), localDeviceId, localName, text, MessageType.TEXT, channel = channel)
    )

    fun localLocation(latitude: Double, longitude: Double, accuracy: Float?) = SyncPacket.Location(localDeviceId, latitude, longitude, accuracy, System.currentTimeMillis())

    fun localPresence(batteryPercent: Int, channel: ConnectionChannel, online: Boolean = true) =
        SyncPacket.Presence(localDeviceId, localName, online, batteryPercent.coerceIn(0, 100), channel)

    fun sendCommandMessage(session: SecurePeerSession, message: CommandMessage): Result<Unit> =
        send(session, SyncPacket.CommandMessagePacket(message))

    fun sendCommandTask(session: SecurePeerSession, task: CommandTask): Result<Unit> =
        send(session, SyncPacket.CommandTaskPacket(task))

    fun sendCommandMap(session: SecurePeerSession, packet: CommandMapPacket): Result<Unit> =
        send(session, SyncPacket.CommandMapPacketMessage(packet))

    private fun apply(packet: SyncPacket) {
        when (packet) {
            is SyncPacket.Location -> update(packet.deviceId) { it.copy(latitude = packet.latitude, longitude = packet.longitude, lastSeen = packet.timestamp) }
            is SyncPacket.Presence -> update(packet.deviceId) { it.copy(name = packet.name, online = packet.online, batteryPercent = packet.batteryPercent, channel = packet.channel, lastSeen = System.currentTimeMillis()) }
            is SyncPacket.CommandMessagePacket -> collaborationStore.addMessage(packet.item)
            is SyncPacket.CommandTaskPacket -> collaborationStore.upsertTask(packet.item)
            is SyncPacket.CommandMapPacketMessage -> commandMapStore.apply(packet.item)
            else -> Unit
        }
    }

    private fun update(deviceId: String, transform: (PeerSnapshot) -> PeerSnapshot) {
        val index = peers.indexOfFirst { it.deviceId == deviceId }
        if (index >= 0) peers[index] = transform(peers[index])
        else peers += transform(PeerSnapshot(deviceId, deviceId, true, 0, ConnectionChannel.OFFLINE))
    }

    fun close() { receiveJob?.cancel(); scope.cancel() }
}
