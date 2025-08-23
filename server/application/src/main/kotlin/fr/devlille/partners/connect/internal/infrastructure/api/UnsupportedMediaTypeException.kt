package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode

/**
 * Exception thrown when the media type of a request is not supported.
 * Includes structured error code and metadata for better error handling.
 */
class UnsupportedMediaTypeException(
    val code: ErrorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE,
    override val message: String = "Unsupported media type",
    val status: HttpStatusCode = HttpStatusCode.UnsupportedMediaType,
    val meta: Map<String, String> = emptyMap(),
) : Throwable(message) {
    // Backward compatibility constructor
    constructor(message: String) : this(
        code = ErrorCode.UNSUPPORTED_MEDIA_TYPE,
        message = message,
        status = HttpStatusCode.UnsupportedMediaType,
        meta = emptyMap(),
    )
}
