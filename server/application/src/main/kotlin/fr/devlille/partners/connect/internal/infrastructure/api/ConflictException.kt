package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when a request conflicts with the current state of the resource.
 * Includes structured error code and metadata for better error handling.
 */
class ConflictException(
    val code: ErrorCode,
    override val message: String = "Conflict with current resource state",
    val status: HttpStatusCode = HttpStatusCode.Conflict,
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
