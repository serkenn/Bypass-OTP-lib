package com.example.plusmessageotp

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/**
 * NanoHTTPD ベースの HTTP API サーバー (ポート8765)。
 *
 * エンドポイント:
 *   GET  /health       - 疎通確認
 *   GET  /otp          - 現在のOTP確認（非消費）
 *   POST /otp/consume  - OTP取得＆消費（1回限り）
 *   POST /otp/clear    - OTPクリア
 */
class OtpHttpServer(port: Int = 8765) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            method == Method.GET && uri == "/health" -> jsonResponse(
                "status" to "ok"
            )

            method == Method.GET && uri == "/otp" -> {
                val code = OtpStore.peek()
                if (code != null) {
                    jsonResponse("otp" to code)
                } else {
                    jsonResponse("otp" to JSONObject.NULL, status = Response.Status.NO_CONTENT)
                }
            }

            method == Method.POST && uri == "/otp/consume" -> {
                val code = OtpStore.consume()
                if (code != null) {
                    jsonResponse("otp" to code)
                } else {
                    jsonResponse("otp" to JSONObject.NULL, status = Response.Status.NO_CONTENT)
                }
            }

            method == Method.POST && uri == "/otp/clear" -> {
                OtpStore.clear()
                jsonResponse("status" to "cleared")
            }

            else -> jsonResponse(
                "error" to "not found",
                status = Response.Status.NOT_FOUND
            )
        }
    }

    private fun jsonResponse(
        vararg pairs: Pair<String, Any?>,
        status: Response.Status = Response.Status.OK,
    ): Response {
        val json = JSONObject().apply {
            for ((k, v) in pairs) put(k, v)
        }
        return newFixedLengthResponse(
            status,
            "application/json",
            json.toString(),
        )
    }
}
