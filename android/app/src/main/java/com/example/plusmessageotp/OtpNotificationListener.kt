package com.example.plusmessageotp

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * +Message (RCS) 通知からOTPを抽出する NotificationListenerService。
 *
 * 対応パッケージ:
 * - DoCoMo:   com.nttdocomo.android.msg
 * - au:       com.kddi.android.cmail
 * - SoftBank: jp.softbank.mb.plusmessage
 */
class OtpNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "OtpListener"

        private val PLUS_MESSAGE_PACKAGES = setOf(
            "com.nttdocomo.android.msg",
            "com.kddi.android.cmail",
            "jp.softbank.mb.plusmessage",
        )

        // 6桁数字を抽出する正規表現
        private val OTP_PATTERN = Regex("""\b(\d{6})\b""")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName !in PLUS_MESSAGE_PACKAGES) return

        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: return

        Log.d(TAG, "通知テキスト: $text")

        val match = OTP_PATTERN.find(text)
        if (match != null) {
            val code = match.groupValues[1]
            Log.i(TAG, "OTP検出: $code")
            OtpStore.set(code)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 何もしない
    }
}
