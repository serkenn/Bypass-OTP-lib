package com.example.plusmessageotp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Foreground Service: HTTP サーバーの生存を保証する。
 */
class OtpHttpService : Service() {

    companion object {
        private const val TAG = "OtpHttpService"
        private const val CHANNEL_ID = "otp_http_service"
        private const val NOTIFICATION_ID = 1
    }

    private var server: OtpHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        server = OtpHttpServer().also {
            it.start()
            Log.i(TAG, "HTTP server started on port 8765")
        }
    }

    override fun onDestroy() {
        server?.stop()
        Log.i(TAG, "HTTP server stopped")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OTP HTTP Server",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "OTP配信用HTTPサーバーを実行中"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OTP Server")
            .setContentText("ポート8765で待機中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
