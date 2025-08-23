package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when the request contains invalid or malformed data.
 * Includes structured error code and metadata for better error handling.
 */
class BadRequestException(
    val code: ErrorCode,
    override val message: String = "Bad request",
    val status: HttpStatusCode = HttpStatusCode.BadRequest,
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
