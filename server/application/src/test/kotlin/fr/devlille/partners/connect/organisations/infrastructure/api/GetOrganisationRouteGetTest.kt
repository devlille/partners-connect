package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetOrganisationRouteGetTest {
    @Test
    fun `GET returns 404 when organisation does not exist`() = testApplication {
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/orgs/non-existing-slug")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
