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
    val meta: Map<MetaKey, String> = emptyMap(),
) : Throwable(message)
