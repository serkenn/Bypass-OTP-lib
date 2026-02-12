package com.example.plusmessageotp

import java.util.concurrent.atomic.AtomicReference

/**
 * スレッドセーフなOTP保持。5分で自動失効。
 */
object OtpStore {

    private const val EXPIRY_MS = 5 * 60 * 1000L // 5分

    private data class Entry(val code: String, val timestamp: Long)

    private val ref = AtomicReference<Entry?>(null)

    /** OTPを保存 */
    fun set(code: String) {
        ref.set(Entry(code, System.currentTimeMillis()))
    }

    /** OTPを参照（非消費） */
    fun peek(): String? {
        val entry = ref.get() ?: return null
        if (System.currentTimeMillis() - entry.timestamp > EXPIRY_MS) {
            ref.compareAndSet(entry, null)
            return null
        }
        return entry.code
    }

    /** OTPを取得して消費（1回限り） */
    fun consume(): String? {
        val entry = ref.getAndSet(null) ?: return null
        if (System.currentTimeMillis() - entry.timestamp > EXPIRY_MS) {
            return null
        }
        return entry.code
    }

    /** OTPをクリア */
    fun clear() {
        ref.set(null)
    }
}
