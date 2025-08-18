package fr.devlille.partners.connect.openapi

import fr.devlille.partners.connect.ApplicationConfig
import fr.devlille.partners.connect.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAPITest {
    @Test
    fun `GET openapi returns Swagger UI when OpenAPI is enabled`() = testApplication {
        application {
            module(
                ApplicationConfig(
                    databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                    enableOpenAPI = true,
                    modules = emptyList(),
                ),
            )
        }

        val response = client.get("/openapi")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue("Response should contain OpenAPI documentation") {
            body.contains("Partners Connect API")
        }
    }

    @Test
    fun `GET openapi returns 404 when OpenAPI is disabled`() = testApplication {
        application {
            module(
                ApplicationConfig(
                    databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                    enableOpenAPI = false,
                    modules = emptyList(),
                ),
            )
        }

        val response = client.get("/openapi")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}