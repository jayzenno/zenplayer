package com.zenplayer.tv.data.epg

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object XmlTvTime2 {
    fun parse(value: String): Long? {
        val raw = value.trim()
        if (raw.isBlank()) return null
        runCatching {
            SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply { isLenient = false }.parse(raw)?.time
        }.getOrNull()?.let { return it }
        return runCatching {
            SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(raw)?.time
        }.getOrNull()
    }
}
