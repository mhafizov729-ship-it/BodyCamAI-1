package com.example.bodycamai

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bodycamai.core.AppProfile
import com.example.bodycamai.core.AiFrameResult
import com.example.bodycamai.core.Module
import com.example.bodycamai.core.ConnectionChannel
import com.example.bodycamai.core.TeamMember
import com.example.bodycamai.core.SharedTask
import com.example.bodycamai.core.TaskStatus
import com.example.bodycamai.core.GroupMessage
import com.example.bodycamai.core.MessageType
import com.example.bodycamai.core.PermissionSet
import com.example.bodycamai.network.NetworkChannelMonitor
import com.example.bodycamai.core.chooseBestConnection
import com.example.bodycamai.sensors.Diagnostics
import com.example.bodycamai.sensors.DeviceTelemetryManager
import com.example.bodycamai.map.MapMode
import com.example.bodycamai.map.OfflineMapManager
import com.example.bodycamai.archive.ArchiveManager
import com.example.bodycamai.archive.ArchiveType
import com.example.bodycamai.events.AiEventStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import kotlin.math.roundToInt

private val Cyan = Color(0xFF00E5FF)
private val Panel = Color(0xD9141A20)
private val Green = Color(0xFF63FF8B)
private val Amber = Color(0xFFFFC857)

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        setContent { BodyCamApp() }
    }
}

@Composable
private fun BodyCamApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val networkMonitor = remember { NetworkChannelMonitor(context) }
    val networkState by networkMonitor.state.collectAsState()
    DisposableEffect(networkMonitor) { onDispose { networkMonitor.close() } }
    val telemetryManager = remember { DeviceTelemetryManager(context) }
    val telemetry by telemetryManager.state.collectAsState()
    DisposableEffect(telemetryManager) { telemetryManager.start(); onDispose { telemetryManager.stop() } }
    var recording by remember { mutableStateOf(false) }
    var aiEnabled by remember { mutableStateOf(true) }
    var mapEnabled by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var mapMode by remember { mutableStateOf(MapMode.TOP_DOWN) }
    val offlineMaps = remember { OfflineMapManager() }
    var showProfiles by remember { mutableStateOf(false) }
    var showModules by remember { mutableStateOf(false) }
    var showLive by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var showCommand by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(AppProfile.BODYCAM) }
    var aiResult by remember { mutableStateOf(AiFrameResult()) }
    var lastAiEventSavedAt by remember { mutableLongStateOf(0L) }
    var activeCaptureId by remember { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val enabledModules = remember { mutableStateMapOf<Module, Boolean>().apply { Module.entries.forEach { this[it] = it in setOf(Module.AI, Module.PEOPLE, Module.VEHICLES, Module.MAP, Module.ARCHIVE, Module.EVENTS) } } }

    MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Panel, primary = Cyan)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onRecordingChanged = { recording = it },
                onError = { cameraError = it.message ?: "Ошибка камеры" },
                onAiResult = { result ->
                    if (aiEnabled) {
                        aiResult = result
                        val now = System.currentTimeMillis()
                        val hasEvent = result.objects.isNotEmpty() || result.faceCount > 0 || result.text.isNotBlank()
                        if (hasEvent && now - lastAiEventSavedAt >= 2000L) {
                            lastAiEventSavedAt = now
                            AiEventStore(context).add(
                                result,
                                timestamp = now,
                                latitude = telemetry.latitude,
                                longitude = telemetry.longitude,
                                captureId = activeCaptureId
                            )
                        }
                    }
                },
                onCaptureIdChanged = { activeCaptureId = it }
            )
            HudOverlay(recording, aiEnabled, mapEnabled, aiResult, selectedProfile.title, telemetry)

            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatusPill(if (recording) "● REC" else "READY", if (recording) Color.Red else Color.White)
                    StatusPill("${selectedProfile.title} • AI ${if (aiEnabled) "ON" else "OFF"}", Cyan)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { RecordingBridge.photo() }, Modifier.weight(1f)) { Text("PHOTO") }
                    OutlinedButton(onClick = { aiEnabled = !aiEnabled }, Modifier.weight(1f)) { Text("AI") }
                    Button(onClick = { if (recording) RecordingBridge.stop() else RecordingBridge.start() }, Modifier.weight(1.2f), shape = CircleShape) { Text(if (recording) "STOP" else "REC") }
                }
            }

            GearButton(onOpen = { showMenu = true })

            if (showMenu) MenuDialog(onClose = { showMenu = false }, selectedProfile, aiEnabled, mapEnabled, { aiEnabled = it }, { mapEnabled = it },
                onHud = { showMenu = false; showHud = true }, onDevices = { showMenu = false; showDevices = true }, onProfiles = { showMenu = false; showProfiles = true },
                onModules = { showMenu = false; showModules = true }, onCommand = { showMenu = false; showCommand = true }, onLive = { showMenu = false; showLive = true }, onVoice = { showMenu = false; showVoice = true }, onArchive = { showMenu = false; showArchive = true }, onEvents = { showMenu = false; showEvents = true }, onMap = { showMenu = false; showMap = true }, onDiagnostics = { showMenu = false; showDiagnostics = true })
            if (showHud) HudEditor(onClose = { showHud = false })
            if (showDevices) DevicesDialog(onClose = { showDevices = false })
            if (showProfiles) ProfilesDialog(selectedProfile, { selectedProfile = it; showProfiles = false }, { showProfiles = false })
            if (showModules) ModulesDialog(enabledModules, { showModules = false })
            if (showLive) LiveViewDialog(onClose = { showLive = false })
            if (showVoice) VoiceDialog(onClose = { showVoice = false })
            if (showArchive) ArchiveDialog(onClose = { showArchive = false })
            if (showEvents) AiEventsDialog(onClose = { showEvents = false })
            if (showCommand) CommandCenterDialog(telemetry.latitude, telemetry.longitude, onClose = { showCommand = false })
            if (showMap) MapDialog(telemetry.latitude, telemetry.longitude, telemetry.headingDegrees, mapMode, { mapMode = it }, offlineMaps, onClose = { showMap = false })
            if (showDiagnostics) DiagnosticsDialog(onClose = { showDiagnostics = false })
            cameraError?.let { AlertDialog(onDismissRequest = { cameraError = null }, title = { Text("Ошибка") }, text = { Text(it) }, confirmButton = { TextButton({ cameraError = null }) { Text("OK") } }) }
        }
    }
}

@Composable private fun GearButton(onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        var offset by remember { mutableStateOf(Offset(0f, 0f)) }
        var size by remember { mutableFloatStateOf(56f) }
        Box(Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }.align(Alignment.TopEnd).padding(top = 58.dp, end = 12.dp)
            .size(size.dp).background(Color.Black.copy(.52f), CircleShape).border(1.dp, Cyan.copy(.75f), CircleShape)
            .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); offset += drag } }, contentAlignment = Alignment.Center) {
            TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) { Text("⚙", color = Cyan, fontSize = (size / 2.2f).sp) }
        }
    }
}

@Composable private fun MenuDialog(onClose: () -> Unit, profile: AppProfile, ai: Boolean, map: Boolean, setAi: (Boolean) -> Unit, setMap: (Boolean) -> Unit,
    onHud: () -> Unit, onDevices: () -> Unit, onProfiles: () -> Unit, onModules: () -> Unit, onCommand: () -> Unit, onLive: () -> Unit, onVoice: () -> Unit, onArchive: () -> Unit, onEvents: () -> Unit, onMap: () -> Unit, onDiagnostics: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("BODYCAM AI", color = Cyan) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Android 10+ • универсальная система", color = Color.LightGray)
            MenuToggle("🤖 AI", ai, setAi); MenuToggle("🗺️ Карта", map, setMap)
            Button(onClick = onProfiles, Modifier.fillMaxWidth()) { Text("🎛 Профили: ${profile.title}") }
            Button(onClick = onModules, Modifier.fillMaxWidth()) { Text("🧩 Модули") }
            Button(onClick = onCommand, Modifier.fillMaxWidth()) { Text("🧭 Command / Coordinator") }
            Button(onClick = onHud, Modifier.fillMaxWidth()) { Text("HUD / конструктор") }
            Button(onClick = onDevices, Modifier.fillMaxWidth()) { Text("📡 Устройства") }
            Button(onClick = onLive, Modifier.fillMaxWidth()) { Text("📺 Live View") }
            Button(onClick = onVoice, Modifier.fillMaxWidth()) { Text("🎙️ P2P голосовая связь") }
            Text("📡 Медиаканал: адаптивное качество при слабой сети", color = Color.LightGray, fontSize = 11.sp)
            Button(onClick = onArchive, Modifier.fillMaxWidth()) { Text("📂 Архив") }
            Button(onClick = onEvents, Modifier.fillMaxWidth()) { Text("🤖 AI-события") }
            Button(onClick = onMap, Modifier.fillMaxWidth()) { Text("🗺️ Карта и офлайн-регионы") }
            Button(onClick = onDiagnostics, Modifier.fillMaxWidth()) { Text("📊 Диагностика") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}


@Composable private fun CommandCenterDialog(lat: Double?, lon: Double?, onClose: () -> Unit) {
    var selectedMember by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var showPermissions by remember { mutableStateOf(false) }
    var showCreateTask by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    val roomCode = remember { (100000..999999).random().toString() }
    var pendingJoinCode by remember { mutableStateOf("") }
    val available = remember { mutableStateOf(setOf(ConnectionChannel.MOBILE, ConnectionChannel.INTERNET, ConnectionChannel.WIFI_LOCAL, ConnectionChannel.BLUETOOTH)) }
    val activeChannel = chooseBestConnection(available.value)
    val members = remember {
        mutableStateListOf(
            TeamMember("1", "Участник 1", ConnectionChannel.MOBILE, true, true, false, 87),
            TeamMember("2", "Участник 2", ConnectionChannel.INTERNET, true, true, true, 64),
            TeamMember("3", "Участник 3", ConnectionChannel.WIFI_LOCAL, true, true, false, 91),
            TeamMember("4", "Участник 4", ConnectionChannel.OFFLINE, false, false, false, 22)
        )
    }
    val tasks = remember {
        mutableStateListOf(
            SharedTask("t1", "Точка A", "Проверить указанную точку", TaskStatus.NEW, "1", lat, lon),
            SharedTask("t2", "Маршрут B", "Пройти заданный маршрут", TaskStatus.IN_PROGRESS, "2")
        )
    }
    val messages = remember {
        mutableStateListOf(
            GroupMessage("m1", "system", "Система", "Группа подключена", MessageType.SYSTEM, channel = activeChannel)
        )
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("COMMAND / COORDINATOR", color = Cyan) }, text = {
        Column(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Авторизованная группа • координация, связь и обмен данными", color = Color.LightGray, fontSize = 12.sp)
            Text("🟢 Активный канал: ${activeChannel.title}", color = Green)
            Text("📡 Доступно: ${available.value.joinToString { it.title }}", color = Color.Gray, fontSize = 10.sp)
            Text(if (lat != null && lon != null) "📍 Центр: %.5f, %.5f".format(lat, lon) else "📍 Координаты центра недоступны", color = Green)

            Text("Участники", fontWeight = FontWeight.Bold, color = Color.White)
            LazyColumn(Modifier.heightIn(max = 170.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(members) { member ->
                    Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${if (member.online) "🟢" else "🔴"} ${member.name}", color = Color.White)
                                Text("${member.batteryPercent}%", color = Color.Gray, fontSize = 11.sp)
                            }
                            Text("${member.channel.title} • GPS ${if (member.locationShared) "доступен" else "скрыт"} • камера ${if (member.cameraShared) "разрешена" else "закрыта"}", color = Color.LightGray, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { selectedMember = member.id }) { Text("Чат", fontSize = 10.sp) }
                                OutlinedButton(onClick = { if (member.cameraShared) selectedMember = member.id }) { Text("Камера", fontSize = 10.sp) }
                                OutlinedButton(onClick = { selectedMember = member.id; showPermissions = true }) { Text("Доступ", fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { showCreateTask = true }, Modifier.weight(1f)) { Text("+ Задача") }
                OutlinedButton(onClick = { showInvite = true }, Modifier.weight(1f)) { Text("🔐 Пригласить") }
            }
            Text("Задачи / точки / зоны", fontWeight = FontWeight.Bold, color = Color.White)
            tasks.forEach { task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("📍 ${task.title}", color = Color.White); Text("${task.description} • ${task.assignedMemberId ?: "без назначения"}", color = Color.Gray, fontSize = 10.sp) }
                    Text(task.status.title, color = if (task.status == TaskStatus.DONE) Green else Amber, fontSize = 10.sp)
                }
            }

            Text("Связь", fontWeight = FontWeight.Bold, color = Color.White)
            LazyColumn(Modifier.heightIn(max = 80.dp)) {
                items(messages.takeLast(4)) { m ->
                    Text("${m.senderName}: ${m.text}", color = if (m.type == MessageType.SYSTEM) Color.Gray else Color.White, fontSize = 11.sp)
                }
            }
            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text(if (selectedMember == null) "Сообщение группе" else "Сообщение участнику") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    if (message.isNotBlank()) {
                        messages += GroupMessage("m${messages.size + 1}", "commander", "Командир", message, channel = activeChannel)
                        message = ""
                    }
                }, Modifier.weight(1f)) { Text("Отправить") }
                OutlinedButton(onClick = { messages += GroupMessage("m${messages.size + 1}", "commander", "Командир", "Голосовое сообщение", MessageType.VOICE, channel = activeChannel) }, Modifier.weight(1f)) { Text("🎙 Голос") }
            }
            Text("📷 Камера доступна только при разрешении участника. Мобильная сеть/интернет — дальний канал при наличии покрытия; Wi‑Fi/Bluetooth — локальные.", color = Color.Gray, fontSize = 10.sp)
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })

    if (showInvite) {
        InviteDialog(roomCode = roomCode, pendingJoinCode = pendingJoinCode, onJoinCodeChange = { pendingJoinCode = it }, onClose = { showInvite = false })
    }
    if (showPermissions && selectedMember != null) {
        PermissionDialog(member = members.firstOrNull { it.id == selectedMember }, onClose = { showPermissions = false })
    }
    if (showCreateTask) {
        CreateTaskDialog(onClose = { showCreateTask = false }) { title, desc, memberId ->
            tasks += SharedTask("t${tasks.size + 1}", title, desc, TaskStatus.NEW, memberId, lat, lon)
            showCreateTask = false
        }
    }
}


@Composable private fun InviteDialog(roomCode: String, pendingJoinCode: String, onJoinCodeChange: (String) -> Unit, onClose: () -> Unit) {
    var copied by remember { mutableStateOf(false) }
    var joinStatus by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("🔐 Авторизация группы") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Командир-хост", color = Cyan, fontWeight = FontWeight.Bold)
            Text("Код приглашения", color = Color.Gray, fontSize = 11.sp)
            Text(roomCode, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { copied = true }, Modifier.fillMaxWidth()) { Text(if (copied) "Код готов к передаче" else "Скопировать / показать код") }
            HorizontalDivider()
            Text("Подключение участника", color = Cyan, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = pendingJoinCode, onValueChange = onJoinCodeChange, label = { Text("Код группы") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { joinStatus = if (pendingJoinCode == roomCode) "✅ Код принят. Устройство можно авторизовать." else "⚠️ Код не совпадает." }, Modifier.fillMaxWidth()) { Text("Проверить код") }
            if (joinStatus.isNotBlank()) Text(joinStatus, color = if (joinStatus.startsWith("✅")) Green else Amber, fontSize = 11.sp)
            Text("Код является локальным приглашением. Настоящая авторизация и шифрованное рукопожатие P2P подключаются следующим сетевым слоем.", color = Color.Gray, fontSize = 10.sp)
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable private fun PermissionDialog(member: TeamMember?, onClose: () -> Unit) {
    var location by remember { mutableStateOf(member?.locationShared == true) }
    var camera by remember { mutableStateOf(member?.cameraShared == true) }
    var microphone by remember { mutableStateOf(false) }
    var liveView by remember { mutableStateOf(member?.cameraShared == true) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Разрешения: ${member?.name ?: "участник"}") }, text = {
        Column { MenuToggle("📍 Геопозиция", location) { location = it }; MenuToggle("📷 Камера", camera) { camera = it }; MenuToggle("🎙 Микрофон", microphone) { microphone = it }; MenuToggle("📺 Live View", liveView) { liveView = it } }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Сохранить") } })
}

@Composable private fun CreateTaskDialog(onClose: () -> Unit, onCreate: (String, String, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var member by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Новая задача / точка") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
            OutlinedTextField(desc, { desc = it }, label = { Text("Описание") }, singleLine = true)
            OutlinedTextField(member, { member = it }, label = { Text("ID участника (необязательно)") }, singleLine = true)
            Text("Задача может быть назначена участнику и позже синхронизирована с группой.", color = Color.Gray, fontSize = 10.sp)
        }
    }, confirmButton = { Button(onClick = { if (title.isNotBlank()) onCreate(title, desc, member.ifBlank { null }) }) { Text("Создать") } }, dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } })
}

@Composable private fun MenuToggle(label: String, checked: Boolean, change: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label); Switch(checked, change) }

@Composable private fun ProfilesDialog(selected: AppProfile, onSelect: (AppProfile) -> Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Профили") }, text = {
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.height(360.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AppProfile.entries) { p -> FilterChip(selected == p, { onSelect(p) }, label = { Text(p.title, fontSize = 12.sp) }) }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable private fun ModulesDialog(states: MutableMap<Module, Boolean>, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Модули") }, text = {
        Column(Modifier.heightIn(max = 480.dp)) { Module.entries.forEach { m -> MenuToggle(m.title, states[m] == true) { states[m] = it } } }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Готово") } })
}

@Composable private fun DevicesDialog(onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("Устройства") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📱 Телефон — основная камера", color = Green)
            Text("📷 USB / Wi‑Fi / IP-камера — подключение при наличии Android-интерфейса")
            Text("🥽 AR/XR — камера, HUD и датчики при поддержке устройства")
            Text("🎙️ Внешний микрофон — доступен через Android audio stack")
            Text("🛸 Дрон — через поддерживаемый SDK/API; возможности зависят от модели")
            Text("🔄 Источник можно выбрать автоматически или вручную")
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable
private fun VoiceDialog(onClose: () -> Unit) {
    var connected by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("🎙️ P2P голосовая связь", color = Cyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (connected) "🟢 Голосовой канал активен" else "⚪ Голосовой канал не подключён", color = if (connected) Green else Color.LightGray)
                Text("Запрос связи требует разрешения второго устройства. Реальный аудиотранспорт подключается отдельным media-слоем.", color = Color.Gray, fontSize = 11.sp)
                Button(onClick = { connected = !connected }, Modifier.fillMaxWidth()) { Text(if (connected) "Остановить" else "Запросить связь") }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } }
    )
}

@Composable private fun LiveViewDialog(onClose: () -> Unit) {
    var layout by remember { mutableStateOf("1") }
    AlertDialog(onDismissRequest = onClose, title = { Text("Live View") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("P2P Live View • только с разрешением владельца камеры", color = Cyan)
            Text("Окна: $layout", color = Green)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("1", "2", "4", "6", "9").forEach { value ->
                    OutlinedButton(onClick = { layout = value }) { Text(value) }
                }
            }
            Text("Запрос камеры → участник подтверждает → согласование возможностей → подключение видеопотока.")
            Text("Аудио запрашивается отдельно. Владелец может остановить поток в любой момент.", color = Color.LightGray, fontSize = 12.sp)
            Text("Пока реализован P2P-слой согласования; медиакодек/транспорт подключается отдельным модулем.", color = Amber, fontSize = 11.sp)
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable private fun HudEditor(onClose: () -> Unit) {
    var alpha by remember { mutableFloatStateOf(.85f) }
    var scale by remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.94f))) {
        Text("HUD КОНСТРУКТОР", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(20.dp))
        Box(Modifier.padding(24.dp).align(Alignment.Center).background(Panel.copy(alpha), RoundedCornerShape(12.dp)).padding(18.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("● REC   GPS 40.7128°   AI ON", color = Color.White); Text("🧭 125°    🗺️ MAP    ⚠️ EVENTS", color = Cyan); Text("Размер ${"%.1f".format(scale)} • Прозрачность ${"%.0f".format(alpha * 100)}%", color = Color.LightGray) }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(18.dp)) {
            Text("Размер", color = Color.White); Slider(scale, { scale = it }, valueRange = .7f..1.5f)
            Text("Прозрачность", color = Color.White); Slider(alpha, { alpha = it }, valueRange = .2f..1f)
            Button(onClick = onClose, Modifier.fillMaxWidth()) { Text("Готово") }
        }
    }
}


@Composable private fun MapDialog(lat: Double?, lon: Double?, heading: Float?, mode: MapMode, setMode: (MapMode) -> Unit, manager: OfflineMapManager, onClose: () -> Unit) {
    val regions by manager.regions.collectAsState()
    AlertDialog(onDismissRequest = onClose, title = { Text("КАРТА", color = Cyan) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (lat != null && lon != null) "📍 %.5f, %.5f".format(lat, lon) else "📍 GPS не получен", color = Color.White)
            Text("🧭 Курс: ${heading?.roundToInt()?.let { "$it°" } ?: "—"}", color = Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                MapMode.values().forEach { m -> FilterChip(selected = mode == m, onClick = { setMode(m) }, label = { Text(m.title, fontSize = 10.sp) }) }
            }
            Text(if (regions.any { it.downloaded }) "OFFLINE: доступно" else "OFFLINE: регионы не загружены", color = if (regions.any { it.downloaded }) Green else Amber)
            Text("Загрузка карт выполняется только вручную. Автоматического скачивания нет.", color = Color.LightGray, fontSize = 12.sp)
            regions.forEach { region ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(region.name); Text("${region.sizeMb} MB • ${if (region.downloaded) "загружено" else "не загружено"}", color = Color.Gray, fontSize = 11.sp) }
                    if (region.downloaded) TextButton({ manager.delete(region.id) }) { Text("Удалить") }
                    else TextButton({ manager.markDownloaded(region.id) }) { Text("Подготовить") }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}


@Composable private fun ArchiveDialog(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { ArchiveManager(context) }
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<ArchiveType?>(null) }
    var onlyEvents by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val items = remember(query, type, onlyEvents, refresh) { manager.query(com.example.bodycamai.archive.ArchiveFilter(query, type, if (onlyEvents) true else null)) }
    AlertDialog(onDismissRequest = onClose, title = { Text("АРХИВ", color = Cyan) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Поиск") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(type == null, { type = null }, label = { Text("Все") })
                FilterChip(type == ArchiveType.VIDEO, { type = ArchiveType.VIDEO }, label = { Text("Видео") })
                FilterChip(type == ArchiveType.PHOTO, { type = ArchiveType.PHOTO }, label = { Text("Фото") })
                FilterChip(onlyEvents, { onlyEvents = !onlyEvents }, label = { Text("AI-события") })
            }
            Text("Найдено: ${items.size}", color = Color.LightGray)
            if (items.isEmpty()) Text("Записей BodyCam AI пока нет.", color = Amber)
            else LazyColumn(Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(items) { item ->
                    Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(9.dp)) {
                            Text(if (item.type == ArchiveType.VIDEO) "🎥 ${item.name}" else "📷 ${item.name}", color = Color.White)
                            Text(item.formattedDate(), color = Cyan, fontSize = 11.sp)
                            Text("${item.sizeBytes / 1024 / 1024} MB • ${item.mimeType}", color = Color.Gray, fontSize = 11.sp)
                            if (item.aiEventCount > 0) Text("🤖 AI-событий: ${item.aiEventCount} • capture ${item.captureId}", color = Green, fontSize = 11.sp)
                            else Text("🤖 AI-событий: нет", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { refresh++ }, Modifier.fillMaxWidth()) { Text("Обновить") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable private fun AiEventsDialog(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AiEventStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val events = remember(refresh) { store.list() }
    AlertDialog(onDismissRequest = onClose, title = { Text("AI-СОБЫТИЯ", color = Cyan) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("События сохраняются локально вместе со временем и GPS.", color = Color.LightGray, fontSize = 12.sp)
            Text("Найдено: ${events.size}", color = Green)
            if (events.isEmpty()) Text("Пока нет распознанных событий.", color = Amber)
            else LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(events) { e ->
                    Surface(color = Panel, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.fillMaxWidth().padding(9.dp)) {
                            Text(SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(e.timestamp)), color = Cyan)
                            Text("Объектов: ${e.objectCount} • лиц: ${e.faceCount} • уверенность: ${"%.0f".format(e.maxConfidence * 100)}%", color = Color.White, fontSize = 12.sp)
                            e.captureId?.let { Text("🎥 Связь с записью: $it", color = Green, fontSize = 10.sp) }
                            if (e.latitude != null && e.longitude != null) Text("📍 %.5f, %.5f".format(e.latitude, e.longitude), color = Green, fontSize = 11.sp)
                            if (e.ocr.isNotBlank()) Text("OCR: ${e.ocr}", color = Color.LightGray, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { refresh++ }, Modifier.fillMaxWidth()) { Text("Обновить") }
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

@Composable private fun DiagnosticsDialog(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val r = remember { Diagnostics.run(context, true) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Диагностика") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Камера: ${ok(r.camera)}"); Text("Микрофон: ${ok(r.microphone)}"); Text("GPS: ${ok(r.gps)}"); Text("Компас: ${ok(r.compass)}")
            Text("Память: ${ok(r.storage)}"); Text("AI: ${ok(r.ai)}"); Text("Сеть: ${ok(r.network)}"); Text("ИТОГ: ${ok(r.overall)}")
        }
    }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } })
}

private fun ok(v: Boolean) = if (v) "OK" else "НЕДОСТУПНО"

@Composable private fun StatusPill(text: String, color: Color) { Surface(color = Color.Black.copy(.72f), shape = RoundedCornerShape(10.dp)) { Text(text, color = color, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }
