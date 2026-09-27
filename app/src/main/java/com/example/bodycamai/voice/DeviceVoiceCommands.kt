package com.example.bodycamai.voice

import com.example.bodycamai.drone.DroneCommand

sealed interface DeviceVoiceCommand {
    data class Drone(val command: DroneCommand) : DeviceVoiceCommand
    data class Camera(val command: CameraCommand) : DeviceVoiceCommand
    data object StopAll : DeviceVoiceCommand
}

enum class CameraCommand { LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT, CENTER, START_RECORDING, STOP_RECORDING }

/** Multilingual, language-independent command parser. Parsing never executes a device action. */
class DeviceVoiceCommandParser {
    fun parse(text: String, language: String): DeviceVoiceCommand? {
        val s = normalize(text)
        val w = commandWords(language)
        fun has(key: String) = w[key].orEmpty().any { s.contains(normalize(it)) }
        if (has("stop")) return DeviceVoiceCommand.StopAll
        val cameras = listOf(
            "left" to CameraCommand.LEFT, "right" to CameraCommand.RIGHT, "up" to CameraCommand.UP, "down" to CameraCommand.DOWN,
            "zoom_in" to CameraCommand.ZOOM_IN, "zoom_out" to CameraCommand.ZOOM_OUT, "center" to CameraCommand.CENTER,
            "record_start" to CameraCommand.START_RECORDING, "record_stop" to CameraCommand.STOP_RECORDING
        )
        cameras.firstOrNull { has(it.first) }?.let { return DeviceVoiceCommand.Camera(it.second) }
        if (has("follow")) return DeviceVoiceCommand.Drone(DroneCommand.FOLLOW_OPERATOR)
        if (has("gps_follow")) return DeviceVoiceCommand.Drone(DroneCommand.GPS_FOLLOW)
        if (has("hold")) return DeviceVoiceCommand.Drone(DroneCommand.HOLD_POSITION)
        if (has("return")) return DeviceVoiceCommand.Drone(DroneCommand.RETURN_HOME)
        if (has("camera_follow")) return DeviceVoiceCommand.Drone(DroneCommand.CAMERA_FOLLOW)
        if (has("orbit")) return DeviceVoiceCommand.Drone(DroneCommand.ORBIT)
        return null
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun commandWords(language: String): Map<String, Set<String>> {
        val l = language.lowercase().substringBefore('-')
        return when (l) {
            "ru" -> mapOf("stop" to setOf("стоп", "остановись", "остановить", "прекрати"), "left" to setOf("влево", "налево"), "right" to setOf("вправо", "направо"), "up" to setOf("вверх"), "down" to setOf("вниз"), "zoom_in" to setOf("приблизь", "увеличь"), "zoom_out" to setOf("отдали", "уменьши"), "center" to setOf("центр", "по центру"), "record_start" to setOf("начни запись", "начать запись"), "record_stop" to setOf("останови запись", "стоп запись"), "follow" to setOf("следуй за мной", "следуй за оператором", "сопровождай меня"), "gps_follow" to setOf("следуй по gps", "следуй по джпс"), "hold" to setOf("зависни", "удерживай позицию"), "return" to setOf("вернись домой", "возврат домой"), "camera_follow" to setOf("следи камерой", "камера за мной"), "orbit" to setOf("облети меня", "облет вокруг меня"))
            "es" -> common("alto", "izquierda", "derecha", "arriba", "abajo", "acercar", "alejar", "centro", "iniciar grabación", "detener grabación", "sígueme", "seguir por gps", "mantén posición", "volver a casa", "seguir con cámara", "órbita")
            "de" -> common("stopp", "links", "rechts", "hoch", "runter", "heranzoomen", "herauszoomen", "zentrum", "aufnahme starten", "aufnahme stoppen", "folge mir", "gps folgen", "position halten", "nach hause", "kamera folgen", "umkreisen")
            "fr" -> common("stop", "gauche", "droite", "haut", "bas", "zoomer", "dézoomer", "centre", "commencer l'enregistrement", "arrêter l'enregistrement", "suis-moi", "suivre gps", "maintenir position", "retour maison", "suivre caméra", "orbite")
            "pt" -> common("pare", "esquerda", "direita", "cima", "baixo", "aproximar", "afastar", "centro", "iniciar gravação", "parar gravação", "siga-me", "seguir gps", "manter posição", "voltar para casa", "seguir câmera", "orbitar")
            "it" -> common("stop", "sinistra", "destra", "su", "giù", "ingrandisci", "riduci", "centro", "avvia registrazione", "ferma registrazione", "seguimi", "seguire gps", "mantieni posizione", "torna a casa", "segui con camera", "orbita")
            "tr" -> common("dur", "sol", "sağ", "yukarı", "aşağı", "yaklaştır", "uzaklaştır", "merkez", "kaydı başlat", "kaydı durdur", "beni takip et", "gps takip", "konumu koru", "eve dön", "kamerayla takip", "yörünge")
            "zh" -> common("停止", "左", "右", "上", "下", "放大", "缩小", "中心", "开始录制", "停止录制", "跟随我", "gps跟随", "保持位置", "返航", "相机跟随", "环绕")
            "ja" -> common("停止", "左", "右", "上", "下", "拡大", "縮小", "中央", "録画開始", "録画停止", "私についてきて", "gps追従", "位置保持", "帰還", "カメラ追従", "旋回")
            "ko" -> common("정지", "왼쪽", "오른쪽", "위", "아래", "확대", "축소", "중앙", "녹화 시작", "녹화 중지", "나를 따라와", "gps 추적", "위치 유지", "귀환", "카메라 추적", "선회")
            "ar" -> common("توقف", "يسار", "يمين", "أعلى", "أسفل", "تكبير", "تصغير", "المركز", "بدء التسجيل", "إيقاف التسجيل", "اتبعني", "تتبع gps", "ثبت الموقع", "العودة للمنزل", "تتبع بالكاميرا", "دور")
            "hi" -> common("रुको", "बाएं", "दाएं", "ऊपर", "नीचे", "ज़ूम इन", "ज़ूम आउट", "केंद्र", "रिकॉर्डिंग शुरू", "रिकॉर्डिंग बंद", "मेरा पीछा करो", "gps का अनुसरण", "स्थिति बनाए रखो", "घर लौटो", "कैमरा अनुसरण", "घूमो")
            else -> common("stop", "left", "right", "up", "down", "zoom in", "zoom out", "center", "start recording", "stop recording", "follow me", "gps follow", "hold position", "return home", "camera follow", "orbit")
        }
    }

    private fun common(stop: String, left: String, right: String, up: String, down: String, zoomIn: String, zoomOut: String, center: String, recordStart: String, recordStop: String, follow: String, gpsFollow: String, hold: String, ret: String, cameraFollow: String, orbit: String) = mapOf(
        "stop" to setOf(stop), "left" to setOf(left), "right" to setOf(right), "up" to setOf(up), "down" to setOf(down),
        "zoom_in" to setOf(zoomIn), "zoom_out" to setOf(zoomOut), "center" to setOf(center), "record_start" to setOf(recordStart), "record_stop" to setOf(recordStop),
        "follow" to setOf(follow), "gps_follow" to setOf(gpsFollow), "hold" to setOf(hold), "return" to setOf(ret), "camera_follow" to setOf(cameraFollow), "orbit" to setOf(orbit)
    )
}
