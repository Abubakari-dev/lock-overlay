package com.lockoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var isOverlayActive = false
    private var currentIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OVERLAY -> {
                currentIntent = intent
                startForegroundService()
                showOverlay(intent)
            }
            ACTION_STOP_OVERLAY -> {
                hideOverlay()
                stopForegroundService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lock Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manages the lock overlay feature"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_message))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showOverlay(sourceIntent: Intent?) {
        if (!isOverlayActive) {
            isOverlayActive = true
            val overlayIntent = Intent(this, OverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY

                // Pass bonus feature settings to OverlayActivity
                putExtra("auto_dismiss_enabled", sourceIntent?.getBooleanExtra("auto_dismiss_enabled", false) ?: false)
                putExtra("auto_dismiss_duration", sourceIntent?.getIntExtra("auto_dismiss_duration", 30) ?: 30)
                putExtra("pin_protection_enabled", sourceIntent?.getBooleanExtra("pin_protection_enabled", false) ?: false)
                putExtra("pin_code", sourceIntent?.getStringExtra("pin_code") ?: "")
            }
            startActivity(overlayIntent)

            // Broadcast overlay shown
            sendBroadcast(Intent(ACTION_OVERLAY_SHOWN))
        }
    }

    private fun hideOverlay() {
        if (isOverlayActive) {
            isOverlayActive = false
            // Send broadcast to dismiss overlay
            sendBroadcast(Intent(ACTION_DISMISS_OVERLAY))
            sendBroadcast(Intent(ACTION_OVERLAY_DISMISSED))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    companion object {
        const val ACTION_START_OVERLAY = "lockoverlay.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "lockoverlay.STOP_OVERLAY"
        const val ACTION_DISMISS_OVERLAY = "lockoverlay.DISMISS_OVERLAY"
        const val ACTION_OVERLAY_SHOWN = "lockoverlay.OVERLAY_SHOWN"
        const val ACTION_OVERLAY_DISMISSED = "lockoverlay.OVERLAY_DISMISSED"

        private const val CHANNEL_ID = "lock_overlay_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
