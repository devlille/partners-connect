package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ListUserOrganisationsRouteGetTest {
    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/users/me/orgs")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
