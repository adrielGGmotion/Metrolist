package com.metrolist.music.betterlyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

object TTMLParser {
    
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList()
    )
    
    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean = true
    )
    
    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean
    )
    
    // Helper function to get attribute by local name (handles namespace prefixes)
    private fun Element.getAttributeByLocalName(localName: String): String {
        // First try namespace-aware lookup
        val nsValue = getAttributeNS("http://www.w3.org/ns/ttml#metadata", localName)
        if (nsValue.isNotEmpty()) return nsValue
        
        // Then try with common prefixes
        val prefixedValue = getAttribute("ttm:$localName")
        if (prefixedValue.isNotEmpty()) return prefixedValue
        
        // Finally, search through all attributes
        val attrs = attributes
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val attrName = attr.nodeName ?: continue
            if (attrName == localName || attrName.endsWith(":$localName")) {
                return attr.nodeValue ?: ""
            }
        }
        return ""
    }
    
    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            
            val pElements = doc.getElementsByTagName("p")
            
            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue
                
                val begin = pElement.getAttribute("begin")
                if (begin.isNullOrEmpty()) continue
                
                val startTime = parseTime(begin)
                val spanInfos = mutableListOf<SpanInfo>()
                val backgroundLines = mutableListOf<ParsedLine>()
                
                // Get agent/vocalist info (ttm:agent attribute)
                val agent = pElement.getAttributeByLocalName("agent").ifEmpty { null }
                
                // Parse child nodes to preserve whitespace between spans
                val childNodes = pElement.childNodes
                for (j in 0 until childNodes.length) {
                    val node = childNodes.item(j)
                    
                    when (node.nodeType) {
                        Node.ELEMENT_NODE -> {
                            val span = node as? Element
                            if (span?.tagName?.lowercase() == "span") {
                                // Check for background vocal role (ttm:role="x-bg")
                                val role = span.getAttributeByLocalName("role")
                                
                                when (role) {
                                    "x-bg" -> {
                                        // Parse background vocal line
                                        val bgLine = parseBackgroundSpan(span, startTime)
                                        if (bgLine != null) {
                                            backgroundLines.add(bgLine)
                                        }
                                    }
                                    "x-translation", "x-roman" -> {
                                        // Skip translation and romanization spans
                                    }
                                    else -> {
                                        // Regular word span
                                        val wordBegin = span.getAttribute("begin")
                                        val wordEnd = span.getAttribute("end")
                                        val wordText = span.textContent?.trim() ?: ""
                                        
                                        if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                                            val nextSibling = node.nextSibling
                                            val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE && 
                                                nextSibling.textContent?.contains(Regex("\\s")) == true
                                            
                                            spanInfos.add(
                                                SpanInfo(
                                                    text = wordText,
                                                    startTime = parseTime(wordBegin),
                                                    endTime = parseTime(wordEnd),
                                                    hasTrailingSpace = hasTrailingSpace
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Merge consecutive spans without whitespace between them into single words
                val words = mergeSpansIntoWords(spanInfos)
                val lineText = buildString {
                    words.forEachIndexed { index, word ->
                        append(word.text)
                        if (word.hasTrailingSpace && index < words.lastIndex) {
                            append(" ")
                        }
                    }
                }
                
                // If no spans found, use text content directly (excluding background text)
                val finalText = if (lineText.isEmpty()) {
                    getDirectTextContent(pElement).trim()
                } else {
                    lineText
                }
                
                if (finalText.isNotEmpty()) {
                    lines.add(
                        ParsedLine(
                            text = finalText,
                            startTime = startTime,
                            words = words,
                            agent = agent,
                            isBackground = false,
                            backgroundLines = backgroundLines
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        
        return lines
    }
    
    private fun parseBackgroundSpan(span: Element, parentStartTime: Double): ParsedLine? {
        val bgBegin = span.getAttribute("begin")
        val bgEnd = span.getAttribute("end")
        val bgStartTime = if (bgBegin.isNotEmpty()) parseTime(bgBegin) else parentStartTime
        
        val spanInfos = mutableListOf<SpanInfo>()
        val childNodes = span.childNodes
        
        for (j in 0 until childNodes.length) {
            val node = childNodes.item(j)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val innerSpan = node as? Element
                if (innerSpan?.tagName?.lowercase() == "span") {
                    val role = innerSpan.getAttributeByLocalName("role")
                    
                    // Skip translation and romanization spans
                    if (role == "x-translation" || role == "x-roman") continue
                    
                    val wordBegin = innerSpan.getAttribute("begin")
                    val wordEnd = innerSpan.getAttribute("end")
                    val wordText = innerSpan.textContent?.trim() ?: ""
                    
                    if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                        val nextSibling = node.nextSibling
                        val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE && 
                            nextSibling.textContent?.contains(Regex("\\s")) == true
                        
                        spanInfos.add(
                            SpanInfo(
                                text = wordText,
                                startTime = parseTime(wordBegin),
                                endTime = parseTime(wordEnd),
                                hasTrailingSpace = hasTrailingSpace
                            )
                        )
                    }
                }
            }
        }
        
        val words = mergeSpansIntoWords(spanInfos)
        val lineText = buildString {
            words.forEachIndexed { index, word ->
                append(word.text)
                if (word.hasTrailingSpace && index < words.lastIndex) {
                    append(" ")
                }
            }
        }
        
        val finalText = if (lineText.isEmpty()) {
            getDirectTextContent(span).trim()
        } else {
            lineText
        }
        
        return if (finalText.isNotEmpty()) {
            ParsedLine(
                text = finalText,
                startTime = bgStartTime,
                words = words,
                agent = null,
                isBackground = true,
                backgroundLines = emptyList()
            )
        } else null
    }
    
    private fun getDirectTextContent(element: Element): String {
        val sb = StringBuilder()
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                sb.append(node.textContent)
            } else if (node.nodeType == Node.ELEMENT_NODE) {
                val el = node as? Element
                val role = el?.getAttributeByLocalName("role") ?: ""
                // Skip background, translation, and romanization spans
                if (role != "x-bg" && role != "x-translation" && role != "x-roman") {
                    if (el?.tagName?.lowercase() == "span") {
                        sb.append(el.textContent ?: "")
                    }
                }
            }
        }
        return sb.toString()
    }
    
    private fun mergeSpansIntoWords(spanInfos: List<SpanInfo>): List<ParsedWord> {
        if (spanInfos.isEmpty()) return emptyList()
        
        val words = mutableListOf<ParsedWord>()
        var currentText = StringBuilder()
        var currentStartTime = spanInfos[0].startTime
        var currentEndTime = spanInfos[0].endTime
        
        for ((index, span) in spanInfos.withIndex()) {
            if (index == 0) {
                currentText.append(span.text)
                currentStartTime = span.startTime
                currentEndTime = span.endTime
            } else {
                val prevSpan = spanInfos[index - 1]
                if (prevSpan.hasTrailingSpace) {
                    if (currentText.isNotEmpty()) {
                        words.add(
                            ParsedWord(
                                text = currentText.toString().trim(),
                                startTime = currentStartTime,
                                endTime = currentEndTime,
                                hasTrailingSpace = true
                            )
                        )
                    }
                    currentText = StringBuilder(span.text)
                    currentStartTime = span.startTime
                    currentEndTime = span.endTime
                } else {
                    currentText.append(span.text)
                    currentEndTime = span.endTime
                }
            }
        }
        
        if (currentText.isNotEmpty()) {
            val lastSpan = spanInfos.lastOrNull()
            words.add(
                ParsedWord(
                    text = currentText.toString().trim(),
                    startTime = currentStartTime,
                    endTime = currentEndTime,
                    hasTrailingSpace = lastSpan?.hasTrailingSpace ?: false
                )
            )
        }
        
        return words
    }
    
    fun toLRC(lines: List<ParsedLine>): String {
        // First pass: check if we have multiple vocalists
        // If all lines have the same agent (or only v1/default), don't add agent prefix
        val uniqueAgents = lines.mapNotNull { it.agent?.lowercase() }.distinct()
        val hasMultipleVocalists = uniqueAgents.size > 1 || (uniqueAgents.isNotEmpty() && uniqueAgents.first() != "v1")
        
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                val timestamp = String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds)
                
                if (line.words.isNotEmpty()) {
                    val agentTag = if (hasMultipleVocalists) {
                        val agent = line.agent?.lowercase()?.takeIf { it.isNotEmpty() } ?: "v1"
                        "{agent:$agent}"
                    } else ""
                    appendLine("$timestamp$agentTag${line.text}")
                    val wordData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordData>")
                } else {
                    // Only add agent suffix if we have multiple vocalists
                    val agentSuffix = if (hasMultipleVocalists) {
                        when (line.agent?.lowercase()) {
                            "v1" -> "v1: "
                            "v2" -> "v2: "
                            "v3" -> "v3: "
                            "v4" -> "v4: "
                            else -> if (!line.agent.isNullOrEmpty()) "${line.agent}: " else ""
                        }
                    } else {
                        ""
                    }
                    appendLine(String.format("%s%s%s", timestamp, agentSuffix, line.text))
                }
                
                // Add background vocals as separate lines
                line.backgroundLines.forEach { bgLine ->
                    val bgTimeMs = (bgLine.startTime * 1000).toLong()
                    val bgMinutes = bgTimeMs / 60000
                    val bgSeconds = (bgTimeMs % 60000) / 1000
                    val bgCentiseconds = (bgTimeMs % 1000) / 10
                    val bgTimestamp = String.format("[%02d:%02d.%02d]", bgMinutes, bgSeconds, bgCentiseconds)
                    
                    if (bgLine.words.isNotEmpty()) {
                        appendLine("$bgTimestamp{bg}${bgLine.text}")
                        val bgWordData = bgLine.words.joinToString("|") { word ->
                            "${word.text}:${word.startTime}:${word.endTime}"
                        }
                        appendLine("<$bgWordData>")
                    } else {
                        appendLine(String.format("%s{bg}%s", bgTimestamp, bgLine.text))
                    }
                }
            }
        }
    }
    
    private fun parseTime(timeStr: String): Double {
        return try {
            when {
                timeStr.contains(":") -> {
                    val parts = timeStr.split(":")
                    when (parts.size) {
                        2 -> {
                            val minutes = parts[0].toDouble()
                            val seconds = parts[1].toDouble()
                            minutes * 60 + seconds
                        }
                        3 -> {
                            val hours = parts[0].toDouble()
                            val minutes = parts[1].toDouble()
                            val seconds = parts[2].toDouble()
                            hours * 3600 + minutes * 60 + seconds
                        }
                        else -> timeStr.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> timeStr.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
