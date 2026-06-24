package com.arisamtunes.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
)

@Serializable
data class PaginationMeta(
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)
