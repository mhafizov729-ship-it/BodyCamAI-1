package com.example.bodycamai.map

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CommandPointType { CHECKPOINT, EVENT, TASK, SOS, CUSTOM }

data class CommandPoint(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val type: CommandPointType = CommandPointType.CUSTOM,
    val createdBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CommandZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val points: List<Pair<Double, Double>>,
    val createdBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CommandRoute(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val points: List<Pair<Double, Double>>,
    val createdBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class CommandMapPacket {
    data class Point(val point: CommandPoint) : CommandMapPacket()
    data class Zone(val zone: CommandZone) : CommandMapPacket()
    data class Route(val route: CommandRoute) : CommandMapPacket()
    data class Delete(val objectType: String, val id: String) : CommandMapPacket()
}

object CommandMapCodec {
    fun encode(packet: CommandMapPacket): ByteArray {
        val o = JSONObject()
        when (packet) {
            is CommandMapPacket.Point -> o.put("type", "point").put("data", point(packet.point))
            is CommandMapPacket.Zone -> o.put("type", "zone").put("data", zone(packet.zone))
            is CommandMapPacket.Route -> o.put("type", "route").put("data", route(packet.route))
            is CommandMapPacket.Delete -> o.put("type", "delete").put("objectType", packet.objectType).put("id", packet.id)
        }
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): CommandMapPacket {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        return when (o.getString("type")) {
            "point" -> CommandMapPacket.Point(readPoint(o.getJSONObject("data")))
            "zone" -> CommandMapPacket.Zone(readZone(o.getJSONObject("data")))
            "route" -> CommandMapPacket.Route(readRoute(o.getJSONObject("data")))
            "delete" -> CommandMapPacket.Delete(o.getString("objectType"), o.getString("id"))
            else -> error("Неизвестный тип командного объекта")
        }
    }

    private fun point(p: CommandPoint) = JSONObject().apply {
        put("id", p.id); put("title", p.title); put("lat", p.latitude); put("lon", p.longitude)
        put("type", p.type.name); put("createdBy", p.createdBy); put("timestamp", p.timestamp)
    }
    private fun zone(z: CommandZone) = JSONObject().apply { put("id", z.id); put("name", z.name); put("points", pairs(z.points)); put("createdBy", z.createdBy); put("timestamp", z.timestamp) }
    private fun route(r: CommandRoute) = JSONObject().apply { put("id", r.id); put("name", r.name); put("points", pairs(r.points)); put("createdBy", r.createdBy); put("timestamp", r.timestamp) }
    private fun pairs(points: List<Pair<Double, Double>>) = JSONArray().apply { points.forEach { put(JSONArray().put(it.first).put(it.second)) } }
    private fun readPoint(o: JSONObject) = CommandPoint(o.getString("id"), o.getString("title"), o.getDouble("lat"), o.getDouble("lon"), CommandPointType.valueOf(o.getString("type")), o.getString("createdBy"), o.getLong("timestamp"))
    private fun readZone(o: JSONObject) = CommandZone(o.getString("id"), o.getString("name"), readPairs(o.getJSONArray("points")), o.getString("createdBy"), o.getLong("timestamp"))
    private fun readRoute(o: JSONObject) = CommandRoute(o.getString("id"), o.getString("name"), readPairs(o.getJSONArray("points")), o.getString("createdBy"), o.getLong("timestamp"))
    private fun readPairs(a: JSONArray) = (0 until a.length()).map { val p=a.getJSONArray(it); p.getDouble(0) to p.getDouble(1) }
}

class CommandMapStore {
    private val lock = Any()
    private val points = linkedMapOf<String, CommandPoint>()
    private val zones = linkedMapOf<String, CommandZone>()
    private val routes = linkedMapOf<String, CommandRoute>()
    @Synchronized fun apply(packet: CommandMapPacket) { when(packet) {
        is CommandMapPacket.Point -> points[packet.point.id] = packet.point
        is CommandMapPacket.Zone -> zones[packet.zone.id] = packet.zone
        is CommandMapPacket.Route -> routes[packet.route.id] = packet.route
        is CommandMapPacket.Delete -> when(packet.objectType) { "point" -> points.remove(packet.id); "zone" -> zones.remove(packet.id); "route" -> routes.remove(packet.id) }
    }}
    @Synchronized fun points(): List<CommandPoint> = points.values.toList()
    @Synchronized fun zones(): List<CommandZone> = zones.values.toList()
    @Synchronized fun routes(): List<CommandRoute> = routes.values.toList()
    @Synchronized fun clear() { points.clear(); zones.clear(); routes.clear() }
}
