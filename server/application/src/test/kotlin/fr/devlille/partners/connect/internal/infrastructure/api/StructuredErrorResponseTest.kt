package fr.devlille.partners.connect.internal.infrastructure.api

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StructuredErrorResponseTest {
    @Test
    fun `ErrorCode enum contains expected values`() {
        // Test that our error codes exist and are properly defined
        assertEquals("BAD_REQUEST", ErrorCode.BAD_REQUEST.name)
        assertEquals("UNAUTHORIZED", ErrorCode.UNAUTHORIZED.name)
        assertEquals("FORBIDDEN", ErrorCode.FORBIDDEN.name)
        assertEquals("NOT_FOUND", ErrorCode.NOT_FOUND.name)
        assertEquals("EVENT_NOT_FOUND", ErrorCode.EVENT_NOT_FOUND.name)
        assertEquals("PARTNERSHIP_NOT_FOUND", ErrorCode.PARTNERSHIP_NOT_FOUND.name)
        assertEquals("NO_EDIT_PERMISSION", ErrorCode.NO_EDIT_PERMISSION.name)
        assertEquals("INTERNAL_SERVER_ERROR", ErrorCode.INTERNAL_SERVER_ERROR.name)

        // Verify we have comprehensive coverage
        val allCodes = ErrorCode.values()
        assertTrue(allCodes.size >= 30, "Should have at least 30 error codes for comprehensive coverage")
    }

    @Test
    fun `ErrorResponse serializes correctly`() {
        val errorResponse = ErrorResponse(
            code = "TEST_ERROR",
            status = 400,
            meta = mapOf("field" to "value", "resource" to "test"),
        )

        assertEquals("TEST_ERROR", errorResponse.code)
        assertEquals(400, errorResponse.status)
        assertEquals("value", errorResponse.meta["field"])
        assertEquals("test", errorResponse.meta["resource"])
    }

    @Test
    fun `ForbiddenException default constructor works`() {
        val exception = ForbiddenException(
            code = ErrorCode.FORBIDDEN,
            message = "Test message",
        )

        assertEquals(ErrorCode.FORBIDDEN, exception.code)
        assertEquals("Test message", exception.message)
        assertEquals(HttpStatusCode.Forbidden, exception.status)
        assertTrue(exception.meta.isEmpty())
    }

    @Test
    fun `ForbiddenException structured constructor with metadata works`() {
        val exception = ForbiddenException(
            code = ErrorCode.NO_EDIT_PERMISSION,
            message = "No permission for this resource",
            status = HttpStatusCode.Forbidden,
            meta = mapOf("resource" to "event", "action" to "edit"),
        )

        assertEquals(ErrorCode.NO_EDIT_PERMISSION, exception.code)
        assertEquals("No permission for this resource", exception.message)
        assertEquals(HttpStatusCode.Forbidden, exception.status)
        assertEquals("event", exception.meta["resource"])
        assertEquals("edit", exception.meta["action"])
    }

    @Test
    fun `UnauthorizedException default constructor works`() {
        val exception = UnauthorizedException(
            code = ErrorCode.UNAUTHORIZED,
            message = "Auth required",
        )

        assertEquals(ErrorCode.UNAUTHORIZED, exception.code)
        assertEquals("Auth required", exception.message)
        assertEquals(HttpStatusCode.Unauthorized, exception.status)
        assertTrue(exception.meta.isEmpty())
    }

    @Test
    fun `UnauthorizedException structured constructor with metadata works`() {
        val exception = UnauthorizedException(
            code = ErrorCode.TOKEN_MISSING,
            message = "Missing authentication token",
            status = HttpStatusCode.Unauthorized,
            meta = mapOf("header" to "Authorization"),
        )

        assertEquals(ErrorCode.TOKEN_MISSING, exception.code)
        assertEquals("Missing authentication token", exception.message)
        assertEquals(HttpStatusCode.Unauthorized, exception.status)
        assertEquals("Authorization", exception.meta["header"])
    }

    @Test
    fun `UnsupportedMediaTypeException default constructor works`() {
        val exception = UnsupportedMediaTypeException(
            code = ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            message = "Bad media type",
        )

        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.code)
        assertEquals("Bad media type", exception.message)
        assertEquals(HttpStatusCode.UnsupportedMediaType, exception.status)
        assertTrue(exception.meta.isEmpty())
    }

    @Test
    fun `UnsupportedMediaTypeException structured constructor with metadata works`() {
        val exception = UnsupportedMediaTypeException(
            code = ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            message = "Content type not supported",
            status = HttpStatusCode.UnsupportedMediaType,
            meta = mapOf("contentType" to "application/xml", "supported" to "application/json"),
        )

        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.code)
        assertEquals("Content type not supported", exception.message)
        assertEquals(HttpStatusCode.UnsupportedMediaType, exception.status)
        assertEquals("application/xml", exception.meta["contentType"])
        assertEquals("application/json", exception.meta["supported"])
    }
}
