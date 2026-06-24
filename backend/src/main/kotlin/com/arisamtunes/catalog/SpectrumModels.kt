package com.arisamtunes.catalog

import kotlinx.serialization.Serializable

@Serializable
data class SongSpectrumResponse(
    val songId: String,
    val bands: Int,
    val frameDurationMs: Int,
    val frames: List<List<Float>>,
)
