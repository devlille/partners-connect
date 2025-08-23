package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when authentication is required but missing or invalid.
 * Includes structured error code and metadata for better error handling.
 */
class UnauthorizedException(
    val code: ErrorCode = ErrorCode.UNAUTHORIZED,
    override val message: String = "Unauthorized access",
    val status: HttpStatusCode = HttpStatusCode.Unauthorized,
    val meta: Map<String, String> = emptyMap(),
) : Throwable(message) {
    // Backward compatibility constructor
    constructor(message: String) : this(
        code = ErrorCode.UNAUTHORIZED,
        message = message,
        status = HttpStatusCode.Unauthorized,
        meta = emptyMap(),
    )
}
