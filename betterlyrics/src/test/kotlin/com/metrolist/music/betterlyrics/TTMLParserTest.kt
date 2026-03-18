
package com.metrolist.music.betterlyrics

import org.junit.Test
import org.junit.Assert.*

class TTMLParserTest {

    @Test
    fun testV1000AgentMapping() {
        val ttmlBackground = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.500" ttm:agent="v1000">
                    <span>(Background vocal)</span>
                  </p>
                  <p begin="00:02.000" ttm:agent="v1">
                    <span>Main vocal</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlBackground)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue("v1000 should be mapped to {bg}", lrc.contains("{bg}(Background vocal)"))
    }

    @Test
    fun testTimeFormats() {
        val ttmlTimes = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="1.5s">
                    <span>1.5 seconds</span>
                  </p>
                  <p begin="2000ms">
                    <span>2000 milliseconds</span>
                  </p>
                  <p begin="00:03.50">
                    <span>Standard format</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlTimes)
        val lrc = TTMLParser.toLRC(parsedLines)
        val lrcLines = lrc.trim().lines()
        
        assertTrue("1.5s should be [00:01.50]", lrcLines[0].startsWith("[00:01.50]"))
        assertTrue("2000ms should be [00:02.00]", lrcLines[1].startsWith("[00:02.00]"))
        assertTrue("00:03.50 should be [00:03.50]", lrcLines[2].startsWith("[00:03.50]"))
    }

    @Test
    fun testRoleXBgMapping() {
        val ttmlRoleBg = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:05.000">
                    <span ttm:role="x-bg">Background role</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlRoleBg)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue("ttm:role='x-bg' should result in {bg} tag", lrc.contains("{bg}Background role"))
    }

    @Test
    fun testWordLevelSync() {
        val ttmlWordSync = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="00:10.000">
                    <span begin="00:10.000" end="00:10.500">Hello</span>
                    <span begin="00:10.600" end="00:11.000">world</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttmlWordSync)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue("Should contain word-level sync tags", lrc.contains("<Hello:10.0:10.5|world:10.6:11.0>"))
    }

    @Test
    fun testSingleVocalistWithBackground() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000" ttm:agent="v1">
                    <span>Main line</span>
                  </p>
                  <p begin="00:01.200" ttm:agent="v1">
                    <span ttm:role="x-bg">bg</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertFalse("Should not contain agent:v1 when only one main vocalist exists", lrc.contains("{agent:v1}"))
        assertTrue("Should contain {bg} for background vocal", lrc.contains("{bg}bg"))
    }

    @Test
    fun testSingleVocalistNotV1() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <body>
                <div>
                  <p begin="00:01.000" ttm:agent="v2">
                    <span>Only singer is v2</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue("Should contain agent:v2 even if only one, because it's not v1 (implies a part of a larger collaboration)", lrc.contains("{agent:v2}"))
    }

    @Test
    fun testLyricOffset() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <head>
                <metadata>
                  <audio lyricOffset="10.5"/>
                </metadata>
              </head>
              <body>
                <div>
                  <p begin="00:01.000">
                    <span begin="00:01.000" end="00:02.000">Hello</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val parsedLines = TTMLParser.parseTTML(ttml)
        // 1.0 + 10.5 = 11.5 seconds = [00:11.50]
        val lrc = TTMLParser.toLRC(parsedLines)
        
        assertTrue("Timestamp should include offset: [00:11.50] was expected in $lrc", lrc.contains("[00:11.50]"))
        assertTrue("Word data should include offset: 11.5:12.5 was expected", lrc.contains("Hello:11.5:12.5"))
    }
}
