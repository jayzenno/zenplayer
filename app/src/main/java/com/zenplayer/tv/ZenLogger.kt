package com.zenplayer.tv

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Temporary diagnostics logger for development builds. Credentials and URLs are redacted. */
object ZenLogger {
    private const val TAG = "ZenPlayer"
    private const val MAX_BYTES = 512 * 1024L
    private const val MAX_LINES = 2000
    private val lock = Any()
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var file: File? = null

    fun init(context: Context) {
        synchronized(lock) {
            file = File(context.filesDir, "zenplayer-diagnostics.log")
            if (file!!.length() > MAX_BYTES) file!!.writeText("")
            info("APP", "started device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
        }
    }

    fun info(scope: String, message: String) = write("INFO", scope, message)
    fun warn(scope: String, message: String) = write("WARN", scope, message)
    fun error(scope: String, message: String, throwable: Throwable? = null) =
        write("ERROR", scope, message + (throwable?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""))

    fun read(context: Context): String = synchronized(lock) {
        val f = file ?: File(context.filesDir, "zenplayer-diagnostics.log")
        if (!f.exists()) return ""
        f.readLines().takeLast(MAX_LINES).joinToString("\n")
    }

    fun clear(context: Context) = synchronized(lock) {
        val f = file ?: File(context.filesDir, "zenplayer-diagnostics.log")
        f.writeText("")
    }

    fun exportFile(context: Context): File = synchronized(lock) {
        val source = file ?: File(context.filesDir, "zenplayer-diagnostics.log")
        val out = File(context.cacheDir, "zenplayer-diagnostics-${System.currentTimeMillis()}.log")
        out.writeText(source.takeIf { it.exists() }?.readText().orEmpty())
        out
    }

    private fun write(level: String, scope: String, raw: String) {
        val message = redact(raw).take(2000)
        val line = "${time.format(Date())} [$level] [$scope] $message"
        when (level) {
            "ERROR" -> Log.e(TAG, line)
            "WARN" -> Log.w(TAG, line)
            else -> Log.i(TAG, line)
        }
        synchronized(lock) {
            runCatching {
                val f = file ?: return@runCatching
                f.appendText(line + "\n")
                if (f.length() > MAX_BYTES) trim(f)
            }
        }
    }

    private fun trim(f: File) {
        val lines = f.readLines().takeLast(MAX_LINES)
        f.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
    }

    private fun redact(value: String): String = value
        .replace(Regex("(?i)(password|passwd|pwd)\\s*[:=]\\s*[^\\s,;&]+"), "$1=[REDACTED]")
        .replace(Regex("(?i)(username|user)\\s*[:=]\\s*[^\\s,;&]+"), "$1=[REDACTED]")
        .replace(Regex("(?i)(https?://[^\\s]+/live/)[^/\\s]+/[^/\\s]+/"), "$1[REDACTED]/[REDACTED]/")
        .replace(Regex("(?i)(password=)[^&\\s]+"), "$1[REDACTED]")
}
