package de.sgkoenigslutter.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchResponseDto<T>(
    val success: Boolean,
    val count: Int,
    val items: List<T>,
    val errors: List<String>? = null
)
