package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.Serializable

/**
 * Structured error response format for the API.
 * This provides consistent error information that can be used by API consumers
 * for internationalization and specific error handling.
 *
 * @param code The structured error code from ErrorCode enum
 * @param status The HTTP status code
 * @param meta Optional metadata providing additional context about the error
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val status: Int,
    val meta: Map<String, String> = emptyMap(),
)
