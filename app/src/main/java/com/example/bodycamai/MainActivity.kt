package com.example.bodycamai

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bodycamai.archive.ArchiveManager
import com.example.bodycamai.archive.ArchiveType
import com.example.bodycamai.core.*
import androidx.compose.runtime.CompositionLocalProvider
import com.example.bodycamai.events.AiEventStore
import com.example.bodycamai.map.MapMode
import com.example.bodycamai.map.OfflineMapManager
import com.example.bodycamai.network.NetworkChannelMonitor
import com.example.bodycamai.sensors.DeviceTelemetryManager
import com.example.bodycamai.sensors.Diagnostics
import com.example.bodycamai.sensors.adapters.ExternalSensorDiscovery
import com.example.bodycamai.localization.LocalizationManager
import com.example.bodycamai.performance.AdaptivePerformanceController
import com.example.bodycamai.performance.PerformanceMonitor
import com.example.bodycamai.profile.ProfilePolicy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cyan @Composable get() = LocalBodyCamColors.current.primary
private val Panel @Composable get() = LocalBodyCamColors.current.panel
private val Panel2 @Composable get() = LocalBodyCamColors.current.panel2
private val Green @Composable get() = LocalBodyCamColors.current.success
private val Amber @Composable get() = LocalBodyCamColors.current.warning
private val Red @Composable get() = LocalBodyCamColors.current.danger

private enum class AppScreen { HOME, PROFILES, SENSORS, SENSOR_CENTER, AI, MAP, ARCHIVE, EVENTS, DIAGNOSTICS, SETTINGS }

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        setContent { BodyCamApp() }
    }
}

@Composable
private fun BodyCamApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val localization = remember { LocalizationManager(context) }
    val telemetryManager = remember { DeviceTelemetryManager(context) }
    val telemetry by telemetryManager.state.collectAsStateWithLifecycle()
    val networkMonitor = remember { NetworkChannelMonitor(context) }
    val networkState by networkMonitor.state.collectAsStateWithLifecycle()
    val offlineMaps = remember { OfflineMapManager() }
    val sensorDiscovery = remember { ExternalSensorDiscovery(context) }
    val performanceMonitor = remember { PerformanceMonitor(context) }
    val adaptivePerformance = remember { AdaptivePerformanceController() }
    var discoveredSources by remember { mutableStateOf<Map<SensorSource, FusionSource>>(emptyMap()) }

    DisposableEffect(Unit) {
        telemetryManager.start()
        discoveredSources = sensorDiscovery.scan()
        onDispose { telemetryManager.stop(); networkMonitor.close() }
    }

    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var cameraStarted by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var aiEnabled by remember { mutableStateOf(preferences.aiEnabled()) }
    var mapEnabled by remember { mutableStateOf(preferences.mapEnabled()) }
    var frontCamera by remember { mutableStateOf(preferences.frontCamera()) }
    var externalCamera by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(preferences.profile()) }
    var aiResult by remember { mutableStateOf(AiFrameResult()) }
    var aiOverlayMode by remember { mutableStateOf(AiOverlayMode.ALL) }
    var aiAlert by remember { mutableStateOf<AiAlert?>(null) }
    var aiAlertsEnabled by remember { mutableStateOf(preferences.aiAlertsEnabled()) }
    var aiMinConfidence by remember { mutableFloatStateOf(preferences.aiMinConfidence()) }
    var aiLabelsEnabled by remember { mutableStateOf(preferences.aiLabelsEnabled()) }
    var aiContourOnly by remember { mutableStateOf(preferences.aiContourOnly()) }
    val aiAlertEngine = remember(aiMinConfidence) { AiAlertEngine(preferences.aiAlertCooldownMs(), aiMinConfidence) }
    val aiEventStore = remember { AiEventStore(context) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var activeCaptureId by remember { mutableStateOf<String?>(null) }
    var visionMode by remember { mutableStateOf(VisionMode.DAY) }
    var mapMode by remember { mutableStateOf(MapMode.TOP_DOWN) }
    var tracking by remember { mutableStateOf(true) }
    var worldMarkers by remember { mutableStateOf(true) }
    var rearView by remember { mutableStateOf(false) }
    var audioDirection by remember { mutableStateOf(false) }
    var minimap by remember { mutableStateOf(true) }
    var showSensorSheet by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var connectedSources by remember { mutableStateOf(setOf(SensorSource.PHONE_CAMERA)) }
    val fusionPipeline = remember { com.example.bodycamai.core.SensorFusionPipeline(maxDeltaMs = 120L) }
    val aiSmoother = remember { AiTrackingSmoother() }
    val fusedFrameState by fusionPipeline.state.collectAsState()
    var performanceState by remember { mutableStateOf(adaptivePerformance.decide(performanceMonitor.snapshot())) }

    val fusion = remember(visionMode, tracking, worldMarkers, rearView, audioDirection, minimap, connectedSources, discoveredSources, fusedFrameState.health) {
        val liveSources = fusedFrameState.health.filter { it.connected }.map { it.source }.toSet()
        val sourceStates = SensorSource.entries.associateWith { source ->
            val discovered = discoveredSources[source]
            val enabled = source in connectedSources || source in liveSources
            FusionSource(
                source = source,
                connected = enabled,
                latencyMs = discovered?.latencyMs,
                confidence = if (enabled) maxOf(discovered?.confidence ?: 0f, if (source in connectedSources) 1f else 0f) else 0f
            )
        }
        FusionState(
            visionMode = visionMode,
            trackingEnabled = tracking,
            worldMarkersEnabled = worldMarkers,
            rearViewEnabled = rearView,
            audioDirectionEnabled = audioDirection,
            minimapEnabled = minimap,
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            headingDegrees = telemetry.headingDegrees,
            locationAccuracyMeters = telemetry.accuracyMeters,
            sources = sourceStates.values.toList(),
            fusionConfidence = if (connectedSources.size > 1) 0.92f else 1f,
            sourceStates = sourceStates
        )
    }

    LaunchedEffect(Unit) { performanceState = adaptivePerformance.decide(performanceMonitor.snapshot()) }
    LaunchedEffect(aiEnabled) { preferences.setAiEnabled(aiEnabled) }
    LaunchedEffect(mapEnabled) { preferences.setMapEnabled(mapEnabled) }
    LaunchedEffect(frontCamera) { preferences.setFrontCamera(frontCamera) }
    LaunchedEffect(selectedProfile) { preferences.setProfile(selectedProfile) }
    LaunchedEffect(aiAlertsEnabled) { preferences.setAiAlertsEnabled(aiAlertsEnabled) }
    LaunchedEffect(aiMinConfidence) { preferences.setAiMinConfidence(aiMinConfidence) }
    LaunchedEffect(aiLabelsEnabled) { preferences.setAiLabelsEnabled(aiLabelsEnabled) }
    LaunchedEffect(aiContourOnly) { preferences.setAiContourOnly(aiContourOnly) }

    var themePreset by remember { mutableStateOf(preferences.uiTheme()) }
    CompositionLocalProvider(LocalBodyCamColors provides uiTheme(themePreset)) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = LocalBodyCamColors.current.background, surface = LocalBodyCamColors.current.panel, primary = LocalBodyCamColors.current.primary,
        secondary = LocalBodyCamColors.current.success, error = LocalBodyCamColors.current.danger
    )) {
        if (!cameraStarted) {
            MainShell(
                screen = screen,
                selectedProfile = selectedProfile,
                aiEnabled = aiEnabled,
                gpsReady = telemetry.gpsReady,
                networkText = networkState.active.title,
                onScreen = { screen = it },
                onStartCamera = { cameraStarted = true },
                content = {
                    when (screen) {
                        AppScreen.HOME -> HomeContent(selectedProfile, aiEnabled, telemetry.gpsReady, networkState.active.title, onStartCamera = { cameraStarted = true }, onProfiles = { screen = AppScreen.PROFILES }, onSensors = { screen = AppScreen.SENSORS }, onAi = { screen = AppScreen.AI }, onMap = { screen = AppScreen.MAP })
                        AppScreen.PROFILES -> ProfilesContent(selectedProfile) { selectedProfile = it; ProfilePolicy.defaults(it).forEach { module -> preferences.setModuleEnabled(module, true) } }
                        AppScreen.SENSORS -> SensorsContent(fusion, discoveredSources, onRefresh = { discoveredSources = sensorDiscovery.scan() }, onExternal = { externalCamera = true; screen = AppScreen.HOME }, onCenter = { screen = AppScreen.SENSOR_CENTER })
                        AppScreen.SENSOR_CENTER -> SensorCenterContent(fusion, fusedFrameState)
                        AppScreen.AI -> AiContent(aiEnabled, { aiEnabled = it }, tracking, { tracking = it }, worldMarkers, { worldMarkers = it }, aiOverlayMode, { aiOverlayMode = it }, aiAlertsEnabled, { aiAlertsEnabled = it }, aiMinConfidence, { aiMinConfidence = it }, aiLabelsEnabled, { aiLabelsEnabled = it }, aiContourOnly, { aiContourOnly = it })
                        AppScreen.MAP -> MapContent(telemetry, offlineMaps, mapMode, { mapMode = it }, mapEnabled, { mapEnabled = it })
                        AppScreen.ARCHIVE -> ArchiveContent()
                        AppScreen.EVENTS -> EventsContent()
                        AppScreen.DIAGNOSTICS -> DiagnosticsContent(aiEnabled)
                        AppScreen.SETTINGS -> SettingsContent(frontCamera, { frontCamera = it }, visionMode, { visionMode = it }, minimap, { minimap = it }, rearView, { rearView = it }, audioDirection, { audioDirection = it }, themePreset, { themePreset = it; preferences.setUiTheme(it) }, performanceState.tier.name, performanceState.reason)
                    }
                }
            )
        } else {
            CameraScreen(
                recording = recording,
                aiEnabled = aiEnabled,
                frontCamera = frontCamera,
                externalCamera = externalCamera,
                selectedProfile = selectedProfile,
                telemetry = telemetry,
                aiResult = aiResult,
                fusion = fusion,
                fusedFrameState = fusedFrameState,
                fusionPipeline = fusionPipeline,
                onRecordingChanged = { recording = it },
                onCameraError = { cameraError = it },
                onAiResult = {
                    val smoothed = if (tracking) aiSmoother.smooth(it) else it
                    aiResult = smoothed
                    if (aiEnabled && aiAlertsEnabled) aiAlertEngine.evaluate(smoothed)?.also { alert -> aiAlert = alert }
                },
                onCaptureIdChanged = { activeCaptureId = it },
                onBackHome = { cameraStarted = false; screen = AppScreen.HOME },
                onPhoto = { RecordingBridge.photo() },
                onRecord = { if (recording) RecordingBridge.stop() else RecordingBridge.start() },
                onFlip = { frontCamera = !frontCamera },
                onAi = { aiEnabled = !aiEnabled },
                onMode = { showModeSheet = true },
                onSensors = { showSensorSheet = true },
                aiOverlayMode = aiOverlayMode
            )
        }

        if (showSensorSheet) {
            SensorSheet(fusion, onClose = { showSensorSheet = false })
        }
        if (showModeSheet) {
            VisionModeSheet(visionMode, onSelect = { visionMode = it; showModeSheet = false }, onClose = { showModeSheet = false })
        }
        cameraError?.let { error ->
            AlertDialog(onDismissRequest = { cameraError = null }, title = { Text("Ошибка камеры") }, text = { Text(error) }, confirmButton = { TextButton({ cameraError = null }) { Text("Понятно") } })
        }
    }
    }
}

@Composable
private fun MainShell(
    screen: AppScreen,
    selectedProfile: AppProfile,
    aiEnabled: Boolean,
    gpsReady: Boolean,
    networkText: String,
    onScreen: (AppScreen) -> Unit,
    onStartCamera: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column(Modifier.background(Color.Black)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BODYCAM AI", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text("SENSOR FUSION / ${selectedProfile.title.uppercase()}", color = Cyan, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    StatusDot("AI", aiEnabled, Cyan)
                    Spacer(Modifier.width(6.dp))
                    StatusDot("GPS", gpsReady, Green)
                    Spacer(Modifier.width(6.dp))
                    StatusDot("NET", networkText != "Офлайн", Amber)
                }
                HorizontalDivider(color = Cyan.copy(alpha = .15f))
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0E12)) {
                NavItem("⌂", "Главная", screen == AppScreen.HOME) { onScreen(AppScreen.HOME) }
                NavItem("◎", "Профили", screen == AppScreen.PROFILES) { onScreen(AppScreen.PROFILES) }
                NavItem("◈", "Сенсоры", screen == AppScreen.SENSORS || screen == AppScreen.SENSOR_CENTER) { onScreen(AppScreen.SENSORS) }
                NavItem("⚙", "Настройки", screen == AppScreen.SETTINGS) { onScreen(AppScreen.SETTINGS) }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun StatusDot(label: String, active: Boolean, color: Color) {
    Text(label, color = if (active) color else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.border(1.dp, color.copy(alpha = .35f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp))
}

@Composable
private fun HomeContent(selectedProfile: AppProfile, ai: Boolean, gps: Boolean, network: String, onStartCamera: () -> Unit, onProfiles: () -> Unit, onSensors: () -> Unit, onAi: () -> Unit, onMap: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Surface(color = Panel2, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ЕДИНАЯ КАРТИНА", color = Cyan, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    Text("Камера + AI + GPS + внешние сенсоры", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Система объединяет только реально подключённые источники. Тепловизор, дрон и AR/XR добавляются как отдельные модули.", color = Color.LightGray, fontSize = 12.sp)
                    Button(onClick = onStartCamera, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("▶  НАЧАТЬ НАБЛЮДЕНИЕ", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { QuickCard("ПРОФИЛЬ", selectedProfile.title, "Изменить режим и набор функций", "◎", onProfiles) }
        item { QuickCard("СЕНСОРЫ", "1 локальный источник", "Подключить внешнюю камеру / тепло / дрон / AR", "◈", onSensors) }
        item { QuickCard("AI", if (ai) "АКТИВЕН" else "ВЫКЛЮЧЕН", "Объекты • люди • лица • OCR • события", "✦", onAi) }
        item { QuickCard("НАВИГАЦИЯ", if (gps) "GPS ГОТОВ" else "GPS ПОИСК", "Мини-карта • компас • координаты • офлайн", "⌖", onMap) }
        item {
            Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("СЕТЬ", color = Color.Gray, fontSize = 11.sp); Text(network, color = if (network == "Офлайн") Amber else Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, value: String, desc: String, icon: String, onClick: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = Cyan, fontSize = 25.sp, modifier = Modifier.width(42.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.Gray, fontSize = 10.sp); Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(desc, color = Color.LightGray, fontSize = 11.sp) }
            Text("›", color = Cyan, fontSize = 25.sp)
        }
    }
}

@Composable
private fun CameraScreen(
    recording: Boolean, aiEnabled: Boolean, frontCamera: Boolean, externalCamera: Boolean, selectedProfile: AppProfile,
    telemetry: com.example.bodycamai.sensors.DeviceTelemetry, aiResult: AiFrameResult, fusion: FusionState, fusedFrameState: com.example.bodycamai.core.FusedFrameState, fusionPipeline: com.example.bodycamai.core.SensorFusionPipeline,
    onRecordingChanged: (Boolean) -> Unit, onCameraError: (String) -> Unit, onAiResult: (AiFrameResult) -> Unit,
    onCaptureIdChanged: (String?) -> Unit, onBackHome: () -> Unit, onPhoto: () -> Unit, onRecord: () -> Unit,
    onFlip: () -> Unit, onAi: () -> Unit, onMode: () -> Unit, onSensors: () -> Unit, aiOverlayMode: AiOverlayMode
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(), frontCamera = frontCamera, externalCamera = externalCamera,
            onRecordingChanged = onRecordingChanged,
            onError = { onCameraError(it.message ?: "Ошибка камеры") },
            onAiResult = onAiResult, onCaptureIdChanged = onCaptureIdChanged, fusionPipeline = fusionPipeline
        )
        HudOverlay(recording, aiEnabled, aiResult, selectedProfile.title, telemetry, fusion, fusedFrameState, aiOverlayMode, aiAlert)
        Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                CameraChip("‹", onBackHome); CameraChip(if (recording) "● REC" else "READY", onRecord, if (recording) Red else Green)
                CameraChip("AI ${if (aiEnabled) "ON" else "OFF"}", onAi, Cyan); CameraChip(fusion.visionMode.title, onMode, Amber); CameraChip("SENS", onSensors, Cyan)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CameraButton("📷", "Фото", onPhoto)
            CameraButton(if (recording) "■" else "●", if (recording) "Стоп" else "Запись", onRecord, if (recording) Red else Green)
            CameraButton("↻", if (externalCamera) "USB" else "Камера", {
                if (fusion.source(SensorSource.EXTERNAL_CAMERA).connected) {
                    externalCamera = !externalCamera
                } else {
                    onFlip()
                }
            })
        }
    }
}

@Composable
private fun CameraChip(text: String, onClick: () -> Unit, color: Color = Color.White) {
    Surface(color = Color.Black.copy(alpha = .78f), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable(onClick = onClick).border(1.dp, color.copy(alpha = .35f), RoundedCornerShape(8.dp))) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) }
}

@Composable
private fun CameraButton(icon: String, label: String, onClick: () -> Unit, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Surface(color = Color.Black.copy(alpha = .78f), shape = RoundedCornerShape(50.dp), modifier = Modifier.size(54.dp).border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(50.dp))) { Box(contentAlignment = Alignment.Center) { Text(icon, color = color, fontSize = 21.sp) } }
        Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ProfilesContent(selected: AppProfile, onSelect: (AppProfile) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("ПРОФИЛИ", "Каждый профиль меняет набор доступных модулей и HUD.") }
        items(AppProfile.entries.toList()) { profile ->
            SelectCard(profile.title, profile == selected, profileDescription(profile)) { onSelect(profile) }
        }
    }
}

private fun profileDescription(p: AppProfile) = when (p) {
    AppProfile.BODYCAM -> "Запись, AI, GPS, события, архив"
    AppProfile.CIVILIAN -> "Наблюдение и предупреждения без тактических функций"
    AppProfile.POLICE -> "Документирование, объекты, лица, номера и проверка разрешённых данных"
    AppProfile.TACTICAL -> "Мультиисточники, ночной режим, аудио и карта"
    AppProfile.AIRSOFT -> "Игровые цели, игроки, карта и статистика"
    AppProfile.TRAINING -> "Тренировочный режим и анализ"
    AppProfile.SEARCH_RESCUE -> "Поиск, GPS, события, группа и SOS"
    AppProfile.XR -> "AR/XR HUD и внешние устройства"
    AppProfile.COMMAND -> "Координация группы и общая карта"
    AppProfile.CUSTOM -> "Пользовательский набор"
    AppProfile.ALL -> "Все доступные модули"
}

@Composable
private fun SensorsContent(
    fusion: FusionState,
    discovered: Map<SensorSource, FusionSource>,
    onRefresh: () -> Unit,
    onExternal: () -> Unit,
    onCenter: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { SectionTitle("SENSOR FUSION", "Только реальные источники: без фиктивного подключения сенсоров.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("⟳ ОБНОВИТЬ") }
                OutlinedButton(onClick = onCenter, modifier = Modifier.weight(1f)) { Text("SENSOR CENTER") }
            }
        }
        item { MetricCard("Подключено", "${fusion.connectedCount}", "активные источники Fusion") }
        item { MetricCard("Внешняя камера", if (discovered[SensorSource.EXTERNAL_CAMERA]?.connected == true) "ДОСТУПНА" else "НЕ НАЙДЕНА", "Camera2/CameraX external") }
        item { MetricCard("Тепловой USB", if (discovered[SensorSource.THERMAL_CAMERA]?.confidence ?: 0f > 0f) "ОБНАРУЖЕН НАМЁК" else "НЕ ОБНАРУЖЕН", "Подключение ещё требует реального SDK/драйвера") }
        items(SensorSource.entries.toList()) { source ->
            val live = fusion.source(source).connected
            val available = discovered[source]?.connected == true
            Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (live) "●" else if (available) "◐" else "○", color = if (live) Green else if (available) Amber else Color.Gray, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(sourceDescription(source), color = Color.Gray, fontSize = 10.sp)
                    }
                    Text(if (live) "LIVE" else if (available) "READY" else "OFF", color = if (live) Green else if (available) Amber else Color.Gray, fontSize = 10.sp)
                }
            }
        }
        if (discovered[SensorSource.EXTERNAL_CAMERA]?.connected == true) {
            item {
                Button(onClick = onExternal, modifier = Modifier.fillMaxWidth()) {
                    Text("ИСПОЛЬЗОВАТЬ ВНЕШНЮЮ КАМЕРУ")
                }
            }
        }
        item {
            Surface(color = Color(0xFF071A1F), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("МОДУЛЬНАЯ СХЕМА", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("USB-устройство может быть обнаружено Android, но видео/тепловой канал считается активным только после успешного адаптера и живого потока.", color = Color.LightGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SensorCenterContent(fusion: FusionState, frame: FusedFrameState) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("SENSOR CENTER", "Состояние источников, частота/задержка и качество Fusion.") }
        item { MetricCard("Fusion", if (frame.synchronized) "SYNC" else "STANDBY", "Δ ${frame.deltaMs ?: 0} ms • источников ${fusion.connectedCount}") }
        items(fusion.sources) { source ->
            Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(source.source.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (source.connected) "ONLINE" else "OFFLINE", color = if (source.connected) Green else Color.Gray, fontSize = 10.sp)
                    }
                    Text("Confidence: ${"%.0f".format(source.confidence * 100)}%  •  latency: ${source.latencyMs ?: "—"} ms", color = Color.LightGray, fontSize = 11.sp)
                    if (source.source == SensorSource.AUDIO && fusion.audioDirectionEnabled) {
                        Text("Направление: требует многомикрофонного/внешнего аудиосенсора", color = Amber, fontSize = 10.sp)
                    }
                }
            }
        }
        item { Text("REMOTE VIEW: внешний видеопоток появится после подключения совместимого адаптера/SDK. Приложение не имитирует отсутствующее оборудование.", color = Color.Gray, fontSize = 10.sp) }
    }
}

private fun sourceDescription(s: SensorSource) = when (s) {
    SensorSource.PHONE_CAMERA -> "Основной поток CameraX"
    SensorSource.REAR_CAMERA -> "Задняя камера / второй ракурс при поддерживаемом режиме"
    SensorSource.EXTERNAL_CAMERA -> "USB / совместимый внешний видеопоток"
    SensorSource.THERMAL_CAMERA -> "USB / SDK тепловизора"
    SensorSource.DRONE -> "Поток совместимого устройства через разрешённый API"
    SensorSource.XR -> "Внешний AR/XR дисплей и сенсоры"
    SensorSource.AUDIO -> "Микрофон / внешний аудиосенсор"
}

@Composable
private fun SensorRow(name: String, connected: Boolean, description: String, onToggle: () -> Unit = {}) {
    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (connected) "●" else "○", color = if (connected) Green else Color.Gray, fontSize = 18.sp, modifier = Modifier.width(30.dp))
            Column(Modifier.weight(1f)) { Text(name, color = Color.White, fontWeight = FontWeight.Bold); Text(description, color = Color.Gray, fontSize = 10.sp) }
            Text(if (connected) "ON" else "OFF", color = if (connected) Green else Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AiContent(ai: Boolean, setAi: (Boolean) -> Unit, tracking: Boolean, setTracking: (Boolean) -> Unit, markers: Boolean, setMarkers: (Boolean) -> Unit, overlayMode: AiOverlayMode, setOverlayMode: (AiOverlayMode) -> Unit, alerts: Boolean, setAlerts: (Boolean) -> Unit, minConfidence: Float, setMinConfidence: (Float) -> Unit, labels: Boolean, setLabels: (Boolean) -> Unit, contourOnly: Boolean, setContourOnly: (Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("AI PERCEPTION", "Включай категории отдельно и не выдавай предположение за факт.") }
        item { SwitchRow("AI-анализ", "Обработка кадров на устройстве", ai, setAi) }
        item { SwitchRow("Отслеживание", "Стабильные ID объектов в текущем потоке", tracking, setTracking) }
        item { SwitchRow("Мировые маркеры", "Показывать обнаружения в HUD", markers, setMarkers) }
        item { SwitchRow("AI-уведомления", "Показывать события поверх камеры", alerts, setAlerts) }
        item { SwitchRow("Подписи рамок", "Название и confidence рядом с объектом", labels, setLabels) }
        item { SwitchRow("Только контур", "Убрать заливку рамки", contourOnly, setContourOnly) }
        item { Text("МИНИМАЛЬНАЯ УВЕРЕННОСТЬ: ${"%.0f".format(minConfidence * 100)}%", color = Cyan, fontWeight = FontWeight.Bold) }
        item { Slider(value = minConfidence, onValueChange = setMinConfidence, valueRange = 0.1f..0.95f, steps = 16) }
        item { Text("РЕЖИМ ОБВОДКИ", color = Cyan, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) { AiOverlayMode.entries.forEach { mode -> FilterChip(selected = overlayMode == mode, onClick = { setOverlayMode(mode) }, label = { Text(mode.title, fontSize = 10.sp) }) } } }
        items(listOf("Люди", "Лица", "Автомобили", "Номера", "Объекты", "Текст / OCR", "Животные", "Двери / окна", "Свет / вспышки", "События", "Пост-анализ записи", "Описание сцены")) { item -> ToggleLine(item, true) }
        item { Text("Для лиц и номеров приложение показывает результат обнаружения/сопоставления только при наличии соответствующего источника и разрешённой базы. Автоматическое утверждение о преступлении не используется.", color = Color.Gray, fontSize = 10.sp) }
    }
}

@Composable
private fun MapContent(telemetry: com.example.bodycamai.sensors.DeviceTelemetry, manager: OfflineMapManager, selectedMode: MapMode, setMode: (MapMode) -> Unit, enabled: Boolean, setEnabled: (Boolean) -> Unit) {
    val regions by manager.regions.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { SectionTitle("MAP / NAV", "Мини-карта, компас, GPS и ручная подготовка офлайн-регионов.") }
        item { SwitchRow("Карта в HUD", "Показывать мини-карту во время наблюдения", enabled, setEnabled) }
        item { MetricCard("Позиция", telemetry.locationText, "точность ${telemetry.accuracyMeters?.let { "%.1f м".format(it) } ?: "—"}") }
        item { MetricCard("Курс", telemetry.headingText, "скорость ${telemetry.speedKmh?.let { "%.1f км/ч".format(it) } ?: "—"}") }
        item { Text("Режимы карты", color = Cyan, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { MapMode.entries.forEach { mode -> FilterChip(selected = selectedMode == mode, onClick = { setMode(mode) }, label = { Text(mode.title, fontSize = 10.sp) }) } } }
        item { Text("Офлайн-регионы", color = Cyan, fontWeight = FontWeight.Bold) }
        items(regions) { region ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(region.name, color = Color.White); Text("${region.sizeMb} MB", color = Color.Gray, fontSize = 10.sp) }
                Text(if (region.downloaded) "ГОТОВО" else "НЕ ГОТОВО", color = if (region.downloaded) Green else Amber, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ArchiveContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { ArchiveManager(context) }
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<ArchiveType?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val itemsFound = remember(query, type, refresh) { manager.query(com.example.bodycamai.archive.ArchiveFilter(query, type, null)) }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("АРХИВ", "Видео, фото и связанные AI-события.") }
        item { OutlinedTextField(query, { query = it }, label = { Text("Поиск") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { FilterChip(type == null, { type = null }, label = { Text("Все") }); FilterChip(type == ArchiveType.VIDEO, { type = ArchiveType.VIDEO }, label = { Text("Видео") }); FilterChip(type == ArchiveType.PHOTO, { type = ArchiveType.PHOTO }, label = { Text("Фото") }) } }
        item { Text("Найдено: ${itemsFound.size}", color = Color.Gray) }
        items(itemsFound) { item -> Surface(color = Panel, shape = RoundedCornerShape(10.dp)) { Column(Modifier.fillMaxWidth().padding(10.dp)) { Text(item.name, color = Color.White); Text(item.formattedDate(), color = Cyan, fontSize = 10.sp); Text("${item.sizeBytes / 1024 / 1024} MB", color = Color.Gray, fontSize = 10.sp) } } }
        item { OutlinedButton({ refresh++ }, Modifier.fillMaxWidth()) { Text("Обновить") } }
    }
}

@Composable
private fun EventsContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AiEventStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val events = remember(refresh) { store.list() }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("AI EVENTS", "Хронология обнаружений с временем и GPS.") }
        item { Text("Событий: ${events.size}", color = Green) }
        items(events) { e -> Surface(color = Panel, shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(10.dp)) { Text(SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(e.timestamp)), color = Cyan); Text("Объекты ${e.objectCount} • лица ${e.faceCount} • conf ${"%.0f".format(e.maxConfidence * 100)}%", color = Color.White, fontSize = 11.sp); e.latitude?.let { lat -> Text("GPS %.5f, %.5f".format(lat, e.longitude ?: 0.0), color = Green, fontSize = 10.sp) } } } }
        item { OutlinedButton({ refresh++ }, Modifier.fillMaxWidth()) { Text("Обновить") } }
    }
}

@Composable
private fun DiagnosticsContent(ai: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val r = remember(refresh, ai) { Diagnostics.run(context, ai) }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("ДИАГНОСТИКА", "Проверка основных подсистем перед работой.") }
        item { CheckLine("Камера", r.camera) }; item { CheckLine("Микрофон", r.microphone) }; item { CheckLine("GPS", r.gps) }; item { CheckLine("Компас", r.compass) }; item { CheckLine("Хранилище", r.storage) }; item { CheckLine("AI", r.ai) }; item { CheckLine("Сеть", r.network) }
        item { MetricCard("ИТОГ", if (r.overall) "OK" else "ТРЕБУЕТ ВНИМАНИЯ", "повторить проверку после изменения разрешений") }
        item { Button({ refresh++ }, Modifier.fillMaxWidth()) { Text("Повторить") } }
    }
}

@Composable
private fun SettingsContent(front: Boolean, setFront: (Boolean) -> Unit, mode: VisionMode, setMode: (VisionMode) -> Unit, minimap: Boolean, setMinimap: (Boolean) -> Unit, rear: Boolean, setRear: (Boolean) -> Unit, audio: Boolean, setAudio: (Boolean) -> Unit, theme: UiThemePreset, setTheme: (UiThemePreset) -> Unit, performanceTier: String, performanceReason: String) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("НАСТРОЙКИ", "Главные параметры BodyCam AI и HUD.") }
        item { SwitchRow("Фронтальная камера", "Использовать переднюю камеру", front, setFront) }
        item { SwitchRow("Мини-карта", "Показывать карту в рабочем HUD", minimap, setMinimap) }
        item { SwitchRow("Задний обзор", "Резерв под подключаемую rear/side камеру", rear, setRear) }
        item { SwitchRow("Направление звука", "Использовать доступные аудиоданные для приблизительного направления", audio, setAudio) }
        item { Text("ПРОИЗВОДИТЕЛЬНОСТЬ", color = Cyan, fontWeight = FontWeight.Bold) }
        item { MetricCard("Адаптивный режим", performanceTier, performanceReason) }
        item { Text("Система автоматически снижает частоту AI-анализа и число одновременно отображаемых объектов при высокой нагрузке.", color = Color.Gray, fontSize = 10.sp) }
        item { Text("ТЕМА HUD", color = Cyan, fontWeight = FontWeight.Bold) }
        items(UiThemePreset.entries.toList()) { t -> SelectCard(t.name, t == theme, "Цвет окон, HUD, статусов и подсветки") { setTheme(t) } }
        item { Text("Базовая тема — тёмный полупрозрачный HUD в стиле EagleEye; цвета можно менять без изменения логики приложения.", color = Color.Gray, fontSize = 10.sp) }
        item { Text("Режим изображения", color = Cyan, fontWeight = FontWeight.Bold) }
        items(VisionMode.entries.toList()) { v -> SelectCard(v.title, v == mode, visionDescription(v)) { setMode(v) } }
        item { Text("Тепловой и ночной режимы требуют соответствующего сенсора. Интерфейс не имитирует наличие отсутствующего оборудования.", color = Color.Gray, fontSize = 10.sp) }
        item { Text("Безопасность", color = Cyan, fontWeight = FontWeight.Bold) }
        item { Text("Система предназначена для наблюдения и ситуационной осведомлённости. Нет баллистических расчётов, наведения или автономного управления оружием.", color = Color.LightGray, fontSize = 11.sp) }
    }
}

private fun visionDescription(v: VisionMode) = when (v) {
    VisionMode.AUTO -> "Автоматически выбирает доступные источники; не создаёт отсутствующий сенсор"
    VisionMode.DAY -> "Обычный поток камеры"
    VisionMode.LOW_LIGHT -> "Низкая освещённость / ночной сенсор"
    VisionMode.THERMAL -> "Реальный внешний тепловой канал"
    VisionMode.FUSED -> "Объединение доступных визуальных источников"
}

@Composable
private fun SectionTitle(title: String, subtitle: String) { Column(Modifier.padding(bottom = 4.dp)) { Text(title, color = Cyan, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp); Text(subtitle, color = Color.Gray, fontSize = 11.sp) } }

@Composable
private fun MetricCard(title: String, value: String, subtitle: String) { Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, color = Color.Gray, fontSize = 10.sp); Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.LightGray, fontSize = 10.sp) } } }

@Composable
private fun SelectCard(title: String, selected: Boolean, description: String, onClick: () -> Unit) { Surface(color = if (selected) Color(0xFF09262D) else Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).border(1.dp, if (selected) Cyan.copy(alpha = .55f) else Color.White.copy(alpha = .08f), RoundedCornerShape(12.dp))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (selected) "●" else "○", color = if (selected) Cyan else Color.Gray, modifier = Modifier.width(28.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(description, color = Color.Gray, fontSize = 10.sp) } } } }

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(description, color = Color.Gray, fontSize = 10.sp) }; Switch(checked, onCheckedChange = onChecked) } } }

@Composable
private fun ToggleLine(title: String, enabled: Boolean) { Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, modifier = Modifier.weight(1f)); Text(if (enabled) "ON" else "OFF", color = Green, fontSize = 10.sp) } }

@Composable
private fun CheckLine(title: String, ok: Boolean) { Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Color.White); Text(if (ok) "OK" else "НЕТ", color = if (ok) Green else Amber, fontWeight = FontWeight.Bold) } }

@Composable
private fun SensorSheet(fusion: FusionState, onClose: () -> Unit) { AlertDialog(onDismissRequest = onClose, title = { Text("SENSOR FUSION", color = Cyan) }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Локальная камера: подключена", color = Green); Text("Тепловой канал: ${if (fusion.thermalAvailable) "готов" else "не подключён"}"); Text("Внешнее видео: ${if (fusion.externalVideoAvailable) "готово" else "ожидание"}"); Text("Смысл режима FUSED — объединять реальные источники, а не рисовать несуществующие данные.", color = Color.Gray, fontSize = 11.sp) } }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } }) }

@Composable
private fun VisionModeSheet(mode: VisionMode, onSelect: (VisionMode) -> Unit, onClose: () -> Unit) { AlertDialog(onDismissRequest = onClose, title = { Text("РЕЖИМ ВИДЕНИЯ", color = Cyan) }, text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { VisionMode.entries.forEach { v -> SelectCard(v.title, v == mode, visionDescription(v)) { onSelect(v) } } } }, confirmButton = { TextButton(onClick = onClose) { Text("Закрыть") } }) }
}
