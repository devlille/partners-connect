package fr.devlille.partners.connect.internal

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorsConfigurationTest {
    @Test
    fun `OPTIONS request with localhost origin is allowed`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.options("/") {
            header(HttpHeaders.Origin, "http://localhost:3000")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers.contains(HttpHeaders.AccessControlAllowOrigin))
    }

    @Test
    fun `OPTIONS request with localhost on port 8080 is allowed`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.options("/") {
            header(HttpHeaders.Origin, "http://localhost:8080")
            header(HttpHeaders.AccessControlRequestMethod, "POST")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers.contains(HttpHeaders.AccessControlAllowOrigin))
    }

    @Test
    fun `OPTIONS request with 127_0_0_1 origin is allowed`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.options("/") {
            header(HttpHeaders.Origin, "http://127.0.0.1:3000")
            header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers.contains(HttpHeaders.AccessControlAllowOrigin))
    }
}
