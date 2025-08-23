package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when access to a resource is forbidden.
 * Includes structured error code and metadata for better error handling.
 */
class ForbiddenException(
    val code: ErrorCode = ErrorCode.FORBIDDEN,
    override val message: String = "Access forbidden",
    val status: HttpStatusCode = HttpStatusCode.Forbidden,
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
