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
            
            // Parse global offset if available (Apple TTML style)
            var globalOffset = 0.0
            val audioElements = doc.getElementsByTagNameNS("*", "audio")
            if (audioElements.length > 0) {
                val audioEl = audioElements.item(0) as? Element
                val offsetAttr = audioEl?.getAttribute("lyricOffset")
                if (!offsetAttr.isNullOrEmpty()) {
                    globalOffset = offsetAttr.toDoubleOrNull() ?: 0.0
                }
            }

            // Use namespace-aware element lookup
            val pElements = doc.getElementsByTagNameNS("*", "p")
            
            for (i in 0 until pElements.length) {
                val pElement = pElements.item(i) as? Element ?: continue
                
                val begin = pElement.getAttribute("begin")
                if (begin.isNullOrEmpty()) continue
                
                val startTime = parseTime(begin) + globalOffset
                val spanInfos = mutableListOf<SpanInfo>()
                val backgroundLines = mutableListOf<ParsedLine>()
                
                // Get agent/vocalist info (ttm:agent attribute)
                val agent = pElement.getAttributeByLocalName("agent").ifEmpty { null }
                
                // Check if the entire p element is background (ttm:role="x-bg")
                var isPBackground = pElement.getAttributeByLocalName("role") == "x-bg"
                
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
                                        if (isPBackground) {
                                            // If the whole p is background, treat spans as regular words but mark p as background
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
                                                        startTime = parseTime(wordBegin) + globalOffset,
                                                        endTime = parseTime(wordEnd) + globalOffset,
                                                        hasTrailingSpace = hasTrailingSpace
                                                    )
                                                )
                                            }
                                        } else {
                                            // Parse background vocal line nested within this p
                                            val bgLine = parseBackgroundSpan(span, startTime, globalOffset)
                                            if (bgLine != null) {
                                                backgroundLines.add(bgLine)
                                            }
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
                                                    startTime = parseTime(wordBegin) + globalOffset,
                                                    endTime = parseTime(wordEnd) + globalOffset,
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
                
                // If spans were all background but no regular spans, and not already marked, check if we should treat whole p as background
                if (spanInfos.isEmpty() && backgroundLines.isNotEmpty() && getDirectTextContent(pElement).isBlank()) {
                    // This case is handled by backgroundLines.addAll(lines) below if we want them as separate lines,
                    // but for top-level bg we might want to just let it be.
                }

                // If no regular spans but we have background spans, and they are the only content,
                // we might want to promote them if they are simple.
                // But generally paxsenix uses p [agent=v1000] which we already handle in toLRC.
                
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
                            isBackground = isPBackground,
                            backgroundLines = backgroundLines
                        )
                    )
                } else if (backgroundLines.isNotEmpty() && spanInfos.isEmpty()) {
                    // If p has no content of its own but has nested background lines,
                    // just add those background lines directly to the main list if appropriate,
                    // or keep them nested. For simplicity in toLRC, we can just add them.
                    lines.addAll(backgroundLines)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        
        return lines
    }
    
    private fun parseBackgroundSpan(span: Element, parentStartTime: Double, globalOffset: Double): ParsedLine? {
        val bgBegin = span.getAttribute("begin")
        val bgStartTime = if (bgBegin.isNotEmpty()) parseTime(bgBegin) + globalOffset else parentStartTime
        
        val spanInfos = mutableListOf<SpanInfo>()
        
        // Use namespace-aware lookup for inner spans
        val innerSpans = span.getElementsByTagNameNS("*", "span")
        
        if (innerSpans.length == 0) {
            // Check if it's just text
            val text = span.textContent?.trim() ?: ""
            if (text.isNotEmpty()) {
                return ParsedLine(
                    text = text,
                    startTime = bgStartTime,
                    words = emptyList(),
                    agent = null,
                    isBackground = true,
                    backgroundLines = emptyList()
                )
            }
        } else {
            for (i in 0 until innerSpans.length) {
                val innerSpan = innerSpans.item(i) as? Element ?: continue
                val role = innerSpan.getAttributeByLocalName("role")
                
                // Skip translation and romanization spans
                if (role == "x-translation" || role == "x-roman") continue
                
                val wordBegin = innerSpan.getAttribute("begin")
                val wordEnd = innerSpan.getAttribute("end")
                val wordText = innerSpan.textContent?.trim() ?: ""
                
                if (wordText.isNotEmpty() && wordBegin.isNotEmpty() && wordEnd.isNotEmpty()) {
                    val nextSibling = innerSpan.nextSibling
                    val hasTrailingSpace = nextSibling?.nodeType == Node.TEXT_NODE && 
                        nextSibling.textContent?.contains(Regex("\\s")) == true
                    
                    spanInfos.add(
                        SpanInfo(
                            text = wordText,
                            startTime = parseTime(wordBegin) + globalOffset,
                            endTime = parseTime(wordEnd) + globalOffset,
                            hasTrailingSpace = hasTrailingSpace
                        )
                    )
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
        // First pass: check if we have multiple vocalists (excluding background vocals)
        // Only count unique agents assigned to main lines (p tags)
        val mainAgents = lines.asSequence()
            .filter { !it.isBackground }
            .mapNotNull { it.agent?.lowercase() }
            .filter { it != "v1000" && it.isNotEmpty() }
            .distinct()
            .toList()
        
        // Multiple vocalists if more than one unique agent, 
        // OR if there's only one agent but it's NOT v1 (e.g. only v2 is singing)
        val hasMultipleVocalists = mainAgents.size > 1 || 
            (mainAgents.size == 1 && mainAgents.first() != "v1")
        
        return buildString {
            lines.forEach { line ->
                val timeMs = (line.startTime * 1000).toLong()
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                val centiseconds = (timeMs % 1000) / 10
                val timestamp = String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds)
                
                val agentLower = line.agent?.lowercase()
                val isBg = line.isBackground || agentLower == "v1000"
                
                val agentTag = if (isBg) "{bg}" else if (hasMultipleVocalists) {
                    val agent = agentLower?.takeIf { it.isNotEmpty() } ?: "v1"
                    "{agent:$agent}"
                } else ""

                if (line.words.isNotEmpty()) {
                    appendLine("$timestamp$agentTag${line.text}")
                    val wordData = line.words.joinToString("|") { word ->
                        "${word.text}:${word.startTime}:${word.endTime}"
                    }
                    appendLine("<$wordData>")
                } else {
                    appendLine(String.format("%s%s%s", timestamp, agentTag, line.text))
                }
                
                // Add nested background vocals as separate lines with {bg} tag
                line.backgroundLines.forEach { bgLine ->
                    val bgTimeMs = (bgLine.startTime * 1000).toLong()
                    val bgMin = bgTimeMs / 60000
                    val bgSec = (bgTimeMs % 60000) / 1000
                    val bgCs = (bgTimeMs % 1000) / 10
                    val bgTimestamp = String.format("[%02d:%02d.%02d]", bgMin, bgSec, bgCs)
                    
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
            val trimmed = timeStr.trim().lowercase()
            when {
                trimmed.endsWith("ms") -> trimmed.removeSuffix("ms").toDouble() / 1000.0
                trimmed.endsWith("s") -> trimmed.removeSuffix("s").toDouble()
                trimmed.endsWith("m") -> trimmed.removeSuffix("m").toDouble() * 60.0
                trimmed.endsWith("h") -> trimmed.removeSuffix("h").toDouble() * 3600.0
                trimmed.contains(":") -> {
                    val parts = trimmed.split(":")
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
                        else -> trimmed.toDoubleOrNull() ?: 0.0
                    }
                }
                else -> trimmed.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
