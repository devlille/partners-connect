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

/**
 * Integration tests demonstrating the structured error response system.
 * These tests show how the system handles both JSON and text responses
 * based on client Accept headers.
 */
class StructuredErrorIntegrationTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun `test structured error response with JSON Accept header`() = testApplication {
        application {
            moduleMocked()

            // Add a demo route that shows structured error handling
            routing {
                get("/demo/structured-errors") {
                    // Demonstrate different error scenarios
                    val errorType = call.parameters["error"] ?: "forbidden"

                    when (errorType) {
                        "forbidden" -> throw ForbiddenException(
                            code = ErrorCode.NO_EDIT_PERMISSION,
                            message = "You don't have permission to access this resource",
                            meta = mapOf(
                                "resource" to "demo-resource",
                                "action" to "read",
                                "requiredRole" to "admin",
                            ),
                        )
                        "unauthorized" -> throw UnauthorizedException(
                            code = ErrorCode.TOKEN_MISSING,
                            message = "Authentication token is missing",
                            meta = mapOf(
                                "header" to "Authorization",
                                "expectedFormat" to "Bearer <token>",
                            ),
                        )
                        "not-found" -> throw io.ktor.server.plugins.NotFoundException("Resource not found")
                        else -> throw ForbiddenException("Default error")
                    }
                }
            }
        }

        // Test structured forbidden error with JSON response
        val forbiddenResponse = client.get("/demo/structured-errors?error=forbidden") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)
        val forbiddenResponseText = forbiddenResponse.bodyAsText()
        assertTrue(forbiddenResponseText.startsWith("{"))

        val forbiddenError = json.decodeFromString<ErrorResponse>(forbiddenResponseText)
        assertEquals(ErrorCode.NO_EDIT_PERMISSION.name, forbiddenError.code)
        assertEquals(HttpStatusCode.Forbidden.value, forbiddenError.status)
        assertEquals("demo-resource", forbiddenError.meta["resource"])
        assertEquals("read", forbiddenError.meta["action"])
        assertEquals("admin", forbiddenError.meta["requiredRole"])

        // Test structured unauthorized error with JSON response
        val unauthorizedResponse = client.get("/demo/structured-errors?error=unauthorized") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedResponse.status)
        val unauthorizedResponseText = unauthorizedResponse.bodyAsText()
        assertTrue(unauthorizedResponseText.startsWith("{"))

        val unauthorizedError = json.decodeFromString<ErrorResponse>(unauthorizedResponseText)
        assertEquals(ErrorCode.TOKEN_MISSING.name, unauthorizedError.code)
        assertEquals(HttpStatusCode.Unauthorized.value, unauthorizedError.status)
        assertEquals("Authorization", unauthorizedError.meta["header"])
        assertEquals("Bearer <token>", unauthorizedError.meta["expectedFormat"])

        // Test NotFoundException (Ktor exception) with JSON response
        val notFoundResponse = client.get("/demo/structured-errors?error=not-found") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)
        val notFoundResponseText = notFoundResponse.bodyAsText()
        assertTrue(notFoundResponseText.startsWith("{"))

        val notFoundError = json.decodeFromString<ErrorResponse>(notFoundResponseText)
        assertEquals(ErrorCode.NOT_FOUND.name, notFoundError.code)
        assertEquals(HttpStatusCode.NotFound.value, notFoundError.status)
    }

    @Test
    fun `test backward compatibility with text responses`() = testApplication {
        application {
            moduleMocked()

            routing {
                get("/demo/text-errors") {
                    val errorType = call.parameters["error"] ?: "forbidden"

                    when (errorType) {
                        "forbidden" -> throw ForbiddenException("Access denied")
                        "unauthorized" -> throw UnauthorizedException("Login required")
                        else -> throw ForbiddenException("Default error")
                    }
                }
            }
        }

        // Test without Accept header - should get text response
        val forbiddenResponse = client.get("/demo/text-errors?error=forbidden")

        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)
        val forbiddenResponseText = forbiddenResponse.bodyAsText()
        assertFalse(forbiddenResponseText.startsWith("{"))
        assertTrue(forbiddenResponseText.contains("Access denied"))

        // Test unauthorized without Accept header - should get text response
        val unauthorizedResponse = client.get("/demo/text-errors?error=unauthorized")

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedResponse.status)
        val unauthorizedResponseText = unauthorizedResponse.bodyAsText()
        assertFalse(unauthorizedResponseText.startsWith("{"))
        assertTrue(unauthorizedResponseText.contains("Login required"))
    }
}
