package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipRouteBasicTest {

    @Test
    fun `GET partnership route returns 200 with empty array`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        println("Response body: ${response.body<String>()}")
        println("Content-Type: ${response.headers["Content-Type"]}")
    }
}