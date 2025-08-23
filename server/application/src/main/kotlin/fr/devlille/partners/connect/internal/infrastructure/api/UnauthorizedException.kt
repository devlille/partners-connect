package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when authentication is required but missing or invalid.
 * Includes structured error code and metadata for better error handling.
 */
class UnauthorizedException(
    val code: ErrorCode,
    override val message: String = "Unauthorized access",
    val status: HttpStatusCode = HttpStatusCode.Unauthorized,
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
