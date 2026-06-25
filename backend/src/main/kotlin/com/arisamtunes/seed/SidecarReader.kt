package com.arisamtunes.seed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.nameWithoutExtension

class SidecarReader {
    fun read(audio: DiscoveredAudio): Map<String, String> {
        val directory = audio.absolutePath.parent
        val stem = audio.absolutePath.fileName.nameWithoutExtension
        val candidates = listOf(
            "$stem.json", "$stem.yaml", "$stem.yml", "$stem.csv", "$stem.txt",
            "metadata.json", "album.json", "info.json", "tags.txt",
        ).map(directory::resolve).filter(Files::isRegularFile)
        return candidates.fold(emptyMap()) { merged, path -> merged + parse(path).filterKeys { it !in merged } }
    }

    private fun parse(path: Path): Map<String, String> = runCatching {
        when (path.fileName.toString().substringAfterLast('.').lowercase()) {
            "json" -> (Json.parseToJsonElement(Files.readString(path)) as? JsonObject).orEmpty()
                .mapNotNull { (key, value) -> (value as? JsonPrimitive)?.content?.let { key.lowercase() to it } }.toMap()
            "xml" -> parseXml(path)
            "csv" -> parseCsv(path)
            else -> parseKeyValues(path)
        }
    }.getOrDefault(emptyMap())

    private fun parseKeyValues(path: Path) = Files.readAllLines(path).mapNotNull { line ->
        val separator = if (':' in line) ':' else '='
        line.split(separator, limit = 2).takeIf { it.size == 2 }?.let { it[0].trim().lowercase() to it[1].trim() }
    }.toMap()

    private fun parseCsv(path: Path): Map<String, String> {
        val rows = Files.readAllLines(path)
        if (rows.size < 2) return emptyMap()
        return rows[0].split(',').zip(rows[1].split(',')).associate { it.first.trim().lowercase() to it.second.trim().trim('"') }
    }

    private fun parseXml(path: Path): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile())
        val root = document.documentElement
        return (0 until root.childNodes.length).mapNotNull { index ->
            root.childNodes.item(index).takeIf { it.nodeType == org.w3c.dom.Node.ELEMENT_NODE }
                ?.let { it.nodeName.lowercase() to it.textContent.trim() }
        }.toMap()
    }
}
