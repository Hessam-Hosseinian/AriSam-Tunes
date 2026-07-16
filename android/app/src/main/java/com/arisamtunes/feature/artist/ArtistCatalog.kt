package com.arisamtunes.feature.artist

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ArtistProfile(
    val id: String,
    val name: String,
    val image: String,
    @SerialName("image_color") val imageColor: String,
    val bio: String? = null,
    val type: String,
)

data class LocalArtistProfile(
    val profile: ArtistProfile,
    val assetFolder: String,
) {
    val imageUri: String get() = "file:///android_asset/$assetFolder/artist.jpg"
}

@Singleton
class ArtistCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<LocalArtistProfile> = context.assets.list("")
        .orEmpty()
        .mapNotNull(::readArtist)
        .sortedBy { it.profile.name }

    fun artist(id: String): LocalArtistProfile? = all().firstOrNull { it.profile.id == id }

    private fun readArtist(folder: String): LocalArtistProfile? = runCatching {
        val profile = context.assets.open("$folder/artist.json")
            .bufferedReader()
            .use { json.decodeFromString<ArtistProfile>(it.readText()) }
        LocalArtistProfile(profile = profile, assetFolder = folder)
    }.getOrNull()
}
