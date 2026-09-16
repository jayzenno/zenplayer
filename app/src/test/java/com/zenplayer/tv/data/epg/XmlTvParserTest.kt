package com.zenplayer.tv.data.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlTvParserTest {
    @Test
    fun parsesStandardXmlTvProgramme() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <programme start="20260916120000 +0200" stop="20260916130000 +0200" channel="DasErste.de">
                <title>Mittagsmagazin</title>
                <sub-title>Aktuelles</sub-title>
                <desc>Nachrichten und Hintergründe</desc>
                <category>News</category>
                <icon src="https://example.invalid/logo.png"/>
              </programme>
            </tv>
        """.trimIndent()

        val programmes = XmlTvParser.parse(xml, mapOf("DasErste.de" to 7))
        assertEquals(1, programmes.size)
        assertEquals("DasErste.de", programmes[0].channelId)
        assertEquals("Mittagsmagazin", programmes[0].title)
        assertEquals("Aktuelles", programmes[0].subtitle)
        assertEquals("News", programmes[0].category)
        assertEquals("https://example.invalid/logo.png", programmes[0].imageUrl)
        assertTrue(programmes[0].isCatchupAvailable)
        assertEquals(60L * 60L, programmes[0].durationSeconds)
    }

    @Test
    fun ignoresMalformedProgrammeWithoutStart() {
        val xml = "<tv><programme channel=\"x\" stop=\"20260916130000 +0200\"><title>Broken</title></programme></tv>"
        assertTrue(XmlTvParser.parse(xml).isEmpty())
    }
}
