package com.zenplayer.tv

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Development diagnostics logger. It records a bounded structured session so
 * real-TV navigation can be diagnosed without relying on UI instrumentation.
 * Credentials and common playlist URL credentials are redacted before storage.
 */
object ZenLogger {
    private const val TAG = "ZenPlayer"
    private const val MAX_BYTES = 768 * 1024L
    private const val MAX_LINES = 3000
    private const val LOG_FILE = "zenplayer-diagnostics.log"
    private const val SESSION_FILE = "zenplayer-session.log"
    private val lock = Any()
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var file: File? = null
    private var sessionFile: File? = null
    private var sessionId: String = "-"
    private var previousUncaughtHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        synchronized(lock) {
            file = File(context.filesDir, LOG_FILE)
            sessionFile = File(context.filesDir, SESSION_FILE)
            if (file!!.length() > MAX_BYTES) trim(file!!)
            if (sessionFile!!.length() > MAX_BYTES) trim(sessionFile!!)
            sessionId = UUID.randomUUID().toString().take(8)
            info("SESSION", "start id=$sessionId device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT} app=0.1.0")
            installCrashHandler()
        }
    }

    fun info(scope: String, message: String) = write("INFO", scope, message)
    fun warn(scope: String, message: String) = write("WARN", scope, message)
    fun error(scope: String, message: String, throwable: Throwable? = null) =
        write("ERROR", scope, message + (throwable?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""))

    /** Record a hardware/UI event independently of Compose focus state. */
    fun event(type: String, message: String = "") = write("EVENT", type, message)

    fun read(context: Context): String = synchronized(lock) {
        val f = file ?: File(context.filesDir, LOG_FILE)
        if (!f.exists()) return ""
        f.readLines().takeLast(MAX_LINES).joinToString("\n")
    }

    fun readSession(context: Context): String = synchronized(lock) {
        val f = sessionFile ?: File(context.filesDir, SESSION_FILE)
        if (!f.exists()) return ""
        f.readLines().takeLast(MAX_LINES).joinToString("\n")
    }

    fun clear(context: Context) = synchronized(lock) {
        val f = file ?: File(context.filesDir, LOG_FILE)
        f.writeText("")
        val s = sessionFile ?: File(context.filesDir, SESSION_FILE)
        s.writeText("")
    }

    fun exportFile(context: Context): File = synchronized(lock) {
        val source = sessionFile ?: File(context.filesDir, SESSION_FILE)
        val out = File(context.cacheDir, "zenplayer-diagnostics-$sessionId-${System.currentTimeMillis()}.log")
        out.writeText(source.takeIf { it.exists() }?.readText().orEmpty())
        out
    }

    private fun installCrashHandler() {
        if (previousUncaughtHandler != null) return
        previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error("CRASH", "uncaught thread=${thread.name}", throwable)
            runCatching {
                synchronized(lock) {
                    sessionFile?.appendText("${time.format(Date())} [CRASH] [CRASH] ${redact(throwable.stackTraceToString()).take(12000)}\n")
                }
            }
            previousUncaughtHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun write(level: String, scope: String, raw: String) {
        val message = redact(raw).take(2000)
        val line = "${time.format(Date())} [$level] [$scope] [session=$sessionId] $message"
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
                val s = sessionFile ?: return@runCatching
                s.appendText(line + "\n")
                if (s.length() > MAX_BYTES) trim(s)
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
