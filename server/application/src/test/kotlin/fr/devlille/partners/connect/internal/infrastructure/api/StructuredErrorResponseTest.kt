package fr.devlille.partners.connect.internal.infrastructure.api

import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuredErrorResponseTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun `error returns text response by default for backward compatibility`() = testApplication {
        application {
            moduleMocked()
        }

        // Test with an endpoint that requires authorization - should get 401 Unauthorized
        val response = client.get("/users/me/orgs")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val responseText = response.bodyAsText()
        // Should return text response (not JSON) without Accept header
        assertFalse(responseText.startsWith("{"))
        assertTrue(responseText.contains("401") || responseText.contains("Unauthorized"))
    }

    @Test
    fun `error returns JSON response when client accepts JSON`() = testApplication {
        application {
            moduleMocked()
        }

        // Test with an endpoint that requires authorization, requesting JSON response
        val response = client.get("/users/me/orgs") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.startsWith("{"))
        assertTrue(responseText.contains("\"code\""))
        assertTrue(responseText.contains("\"status\""))
        
        // Parse and validate the JSON structure
        val errorResponse = json.decodeFromString<ErrorResponse>(responseText)
        assertEquals(ErrorCode.UNAUTHORIZED.name, errorResponse.code)
        assertEquals(HttpStatusCode.Unauthorized.value, errorResponse.status)
    }

    @Test
    fun `custom exceptions with structured error codes work correctly`() = testApplication {
        application {
            moduleMocked()
            
            // Add a test route that throws our custom exceptions
            routing {
                get("/test-forbidden") {
                    throw ForbiddenException(
                        code = ErrorCode.NO_EDIT_PERMISSION,
                        message = "You don't have permission",
                        meta = mapOf("resource" to "test")
                    )
                }
                get("/test-unauthorized") {
                    throw UnauthorizedException(
                        code = ErrorCode.TOKEN_MISSING,
                        message = "Token required",
                        meta = mapOf("action" to "test")
                    )
                }
            }
        }

        // Test ForbiddenException with JSON response
        val forbiddenResponse = client.get("/test-forbidden") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)
        val forbiddenError = json.decodeFromString<ErrorResponse>(forbiddenResponse.bodyAsText())
        assertEquals(ErrorCode.NO_EDIT_PERMISSION.name, forbiddenError.code)
        assertEquals(HttpStatusCode.Forbidden.value, forbiddenError.status)
        assertEquals("test", forbiddenError.meta["resource"])

        // Test UnauthorizedException with JSON response
        val unauthorizedResponse = client.get("/test-unauthorized") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedResponse.status)
        val unauthorizedError = json.decodeFromString<ErrorResponse>(unauthorizedResponse.bodyAsText())
        assertEquals(ErrorCode.TOKEN_MISSING.name, unauthorizedError.code)
        assertEquals(HttpStatusCode.Unauthorized.value, unauthorizedError.status)
        assertEquals("test", unauthorizedError.meta["action"])
    }

    @Test
    fun `custom exceptions return text for backward compatibility`() = testApplication {
        application {
            moduleMocked()
            
            routing {
                get("/test-forbidden-text") {
                    throw ForbiddenException("You don't have permission")
                }
            }
        }

        // Request without Accept header should get text response
        val response = client.get("/test-forbidden-text")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val responseText = response.bodyAsText()
        assertFalse(responseText.startsWith("{"))
        assertTrue(responseText.contains("permission"))
    }

    @Test
    fun `backward compatibility constructors work correctly`() = testApplication {
        application {
            moduleMocked()
        }

        // Test that old-style exception constructor works
        val forbiddenException = ForbiddenException("Test message")
        assertEquals(ErrorCode.FORBIDDEN, forbiddenException.code)
        assertEquals("Test message", forbiddenException.message)
        assertEquals(HttpStatusCode.Forbidden, forbiddenException.status)
        assertTrue(forbiddenException.meta.isEmpty())

        val unauthorizedException = UnauthorizedException("Test unauthorized")
        assertEquals(ErrorCode.UNAUTHORIZED, unauthorizedException.code)
        assertEquals("Test unauthorized", unauthorizedException.message)
        assertEquals(HttpStatusCode.Unauthorized, unauthorizedException.status)
        assertTrue(unauthorizedException.meta.isEmpty())

        val unsupportedMediaException = UnsupportedMediaTypeException("Test unsupported")
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, unsupportedMediaException.code)
        assertEquals("Test unsupported", unsupportedMediaException.message)
        assertEquals(HttpStatusCode.UnsupportedMediaType, unsupportedMediaException.status)
        assertTrue(unsupportedMediaException.meta.isEmpty())
    }
}