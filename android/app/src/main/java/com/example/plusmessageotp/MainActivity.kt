package com.example.plusmessageotp

import android.content.ComponentName
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvOtp: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvIp = findViewById(R.id.tvIp)
        tvOtp = findViewById(R.id.tvOtp)
        btnPermission = findViewById(R.id.btnPermission)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnRefresh.setOnClickListener {
            refreshStatus()
        }

        // Foreground Service 開始
        startForegroundService(Intent(this, OtpHttpService::class.java))
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val listenerEnabled = isNotificationListenerEnabled()
        tvStatus.text = if (listenerEnabled) {
            "通知アクセス: 許可済み"
        } else {
            "通知アクセス: 未許可（ボタンで設定画面を開いてください）"
        }

        tvIp.text = "IP: ${getLocalIpAddress()}:8765"
        tvOtp.text = "現在のOTP: ${OtpStore.peek() ?: "(なし)"}"
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, OtpNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    private fun getLocalIpAddress(): String {
        try {
            for (intf in NetworkInterface.getNetworkInterfaces()) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "unknown"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "unknown"
    }
}
