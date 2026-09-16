package com.zenplayer.tv.data.epg

import com.zenplayer.tv.domain.model.EpgProgramme
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Parses standard XMLTV programme data into ZenPlayer's normalized EPG model. */
object XmlTvParser {
    fun parse(xml: String, catchupDaysByChannel: Map<String, Int> = emptyMap()): List<EpgProgramme> {
        if (xml.isBlank()) return emptyList()

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }

        val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
        val channelLogos = parseChannelLogos(document)
        val nodes = document.getElementsByTagName("programme")
        val result = ArrayList<EpgProgramme>(nodes.length)

        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val channelId = element.getAttribute("channel").trim()
            val start = parseXmlTvTime(element.getAttribute("start")) ?: continue
            val end = parseXmlTvTime(element.getAttribute("stop")) ?: continue
            if (channelId.isBlank() || end <= start) continue

            val title = childText(element, "title")?.takeIf { it.isNotBlank() } ?: continue
            val subtitle = childText(element, "sub-title")
            val description = childText(element, "desc")
            val category = childText(element, "category")
            val programmeIcon = firstIcon(element)
            val artwork = programmeIcon ?: channelLogos[channelId]

            result += EpgProgramme(
                id = "${channelId.lowercase(Locale.ROOT)}:$start:${title.hashCode()}",
                channelId = channelId,
                title = title,
                subtitle = subtitle,
                description = description,
                start = start,
                end = end,
                category = category,
                imageUrl = artwork,
                isCatchupAvailable = catchupDaysByChannel[channelId]?.let { it > 0 } ?: false,
            )
        }

        return result.sortedWith(compareBy<EpgProgramme> { it.channelId }.thenBy { it.start })
    }

    /** Returns only artwork explicitly supplied by the user's XMLTV file. */
    fun parseChannelLogos(xml: String): Map<String, String> {
        if (xml.isBlank()) return emptyMap()
        val factory = secureFactory()
        val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
        return parseChannelLogos(document)
    }

    private fun parseChannelLogos(document: org.w3c.dom.Document): Map<String, String> {
        val nodes = document.getElementsByTagName("channel")
        val result = LinkedHashMap<String, String>()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val id = element.getAttribute("id").trim()
            val icon = firstIcon(element)
            if (id.isNotBlank() && !icon.isNullOrBlank()) result[id] = icon
        }
        return result
    }

    private fun secureFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
    }

    private fun firstIcon(parent: Element): String? =
        (parent.getElementsByTagName("icon").item(0) as? Element)
            ?.getAttribute("src")?.trim()?.ifBlank { null }

    private fun childText(parent: Element, tag: String): String? =
        (parent.getElementsByTagName(tag).item(0) as? Element)?.textContent?.trim()?.ifBlank { null }

    private fun parseXmlTvTime(value: String): Long? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            runCatching {
                val formatter = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                return formatter.parse(raw)?.time
            }
        }
        return null
    }
}
