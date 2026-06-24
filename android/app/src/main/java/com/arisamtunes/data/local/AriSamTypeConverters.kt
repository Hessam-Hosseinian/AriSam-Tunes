package com.arisamtunes.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AriSamTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> = value?.takeIf(String::isNotBlank)
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrDefault(emptyList()) }
        .orEmpty()
}
