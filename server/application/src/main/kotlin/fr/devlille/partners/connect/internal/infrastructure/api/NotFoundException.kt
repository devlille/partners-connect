package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when a requested resource is not found.
 * Includes structured error code and metadata for better error handling.
 */
class NotFoundException(
    val code: ErrorCode,
    override val message: String = "Resource not found",
    val status: HttpStatusCode = HttpStatusCode.NotFound,
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
