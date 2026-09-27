package com.example.bodycamai.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.bodycamai.R

class BodyCamPersistentService : Service() {
    companion object {
        const val CHANNEL_ID = "bodycam_runtime"
        const val NOTIFICATION_ID = 4601
        const val ACTION_STOP = "com.example.bodycamai.STOP_RUNTIME"
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "BodyCam AI", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Состояние BodyCam AI"
                setShowBadge(false)
            }
        )
        startForeground(NOTIFICATION_ID, notification())
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("BodyCam AI")
            .setContentText("Система работает")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
