package com.example.bodycamai.core

import android.graphics.Rect

enum class AppProfile(val title: String) {
    BODYCAM("BodyCam"), CIVILIAN("Гражданский"), POLICE("Полицейский"),
    TACTICAL("Тактический"), AIRSOFT("Страйкбол"), TRAINING("Тренировка"),
    SEARCH_RESCUE("Search & Rescue"), XR("AR / XR"), COMMAND("Command / Coordinator"), CUSTOM("Мой профиль"), ALL("Все функции")
}

enum class HudElementType { REC, CLOCK, GPS, COMPASS, BATTERY, NETWORK, AI_STATUS, MAP, OBJECTS, DISTANCE, WARNINGS, SPEED, PROFILE }

enum class Module(val title: String) {
    AI("AI"), PEOPLE("Люди"), FACES("Лица"), VEHICLES("Автомобили"), PLATES("Номера"),
    OCR("OCR"), AUDIO("Стерео-аудио"), EVENTS("События"), DRONES("Дроны"), LIVE("Live View"),
    MAP("Карта"), AR("AR / XR"), GROUP("Группа"), SOS("SOS"), NIGHT("Ночной режим"), ARCHIVE("Архив"),
    VOICE("Голос"), DIAGNOSTICS("Диагностика"), ENERGY("Энергосбережение"), SYNC("Синхронизация")
}

data class HudElement(val type: HudElementType, val x: Float, val y: Float, val scale: Float = 1f, val visible: Boolean = true, val alpha: Float = 1f)

data class DetectionBox(val label: String, val confidence: Float, val bounds: Rect, val trackingId: Int? = null, val safetyLabel: String? = null)
enum class AiOverlayMode(val title: String) {
    ALL("Все объекты"),
    PEOPLE("Люди"),
    FACES("Лица"),
    VEHICLES("Транспорт"),
    TEXT("Текст"),
    HIGH_CONFIDENCE("Уверенные")
}

data class WorldMarker(
    val label: String,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val trackingId: Int? = null,
    val source: SensorSource = SensorSource.PHONE_CAMERA
)

data class AiFrameResult(
    val objects: List<DetectionBox> = emptyList(),
    val faceCount: Int = 0,
    val text: String = "",
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
)
data class DiagnosticsResult(val camera: Boolean, val microphone: Boolean, val gps: Boolean, val compass: Boolean, val storage: Boolean, val ai: Boolean, val network: Boolean, val overall: Boolean)


enum class ConnectionChannel(val title: String) {
    INTERNET("Интернет"), MOBILE("4G / 5G"), WIFI_LOCAL("Wi‑Fi / локальная сеть"), BLUETOOTH("Bluetooth"), OFFLINE("Офлайн")
}

enum class GroupNetworkMode(val title: String) {
    LOCAL("Локальная P2P-сеть"), DIRECT_INTERNET("Прямой интернет P2P"), HYBRID("Автоматически")
}

enum class DeliveryStatus { QUEUED, SENDING, DELIVERED, FAILED }

data class BackendConfig(
    val serverUrl: String = "",
    val groupId: String = "",
    val deviceId: String = "",
    val authenticated: Boolean = false
)

enum class TaskStatus(val title: String) { NEW("Новая"), ACCEPTED("Принята"), IN_PROGRESS("В работе"), DONE("Выполнена") }

data class PermissionSet(
    val location: Boolean = true,
    val camera: Boolean = false,
    val microphone: Boolean = false,
    val liveView: Boolean = false,
    val messaging: Boolean = true,
    val routes: Boolean = true
)

data class TeamMember(
    val id: String,
    val name: String,
    val channel: ConnectionChannel = ConnectionChannel.MOBILE,
    val online: Boolean = true,
    val locationShared: Boolean = true,
    val cameraShared: Boolean = false,
    val batteryPercent: Int = 100
)

data class SharedTask(
    val id: String,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.NEW,
    val assignedMemberId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class MessageType { TEXT, VOICE, LOCATION, PHOTO, VIDEO, SYSTEM }

data class GroupMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val channel: ConnectionChannel = ConnectionChannel.OFFLINE
)

data class ConnectionState(
    val preferred: ConnectionChannel = ConnectionChannel.INTERNET,
    val active: ConnectionChannel = ConnectionChannel.OFFLINE,
    val available: Set<ConnectionChannel> = setOf(ConnectionChannel.OFFLINE),
    val queuedMessages: Int = 0
)

fun chooseBestConnection(available: Set<ConnectionChannel>): ConnectionChannel = when {
    ConnectionChannel.MOBILE in available -> ConnectionChannel.MOBILE
    ConnectionChannel.INTERNET in available -> ConnectionChannel.INTERNET
    ConnectionChannel.WIFI_LOCAL in available -> ConnectionChannel.WIFI_LOCAL
    ConnectionChannel.BLUETOOTH in available -> ConnectionChannel.BLUETOOTH
    else -> ConnectionChannel.OFFLINE
}
