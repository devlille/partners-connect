package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EcosystemPartnerCategoryRouteGetTest {
    @Test
    fun `GET orgs returns categories ordered by display_order then name`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catA = UUID.randomUUID()
        val catB = UUID.randomUUID()
        val catC = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                // Display order: 2, 1, null. Sorted: 1 (B), 2 (A), null (C)
                insertMockedEcosystemPartnerCategory(catA, eventId = eventId, name = "Alpha", displayOrder = 2)
                insertMockedEcosystemPartnerCategory(catB, eventId = eventId, name = "Beta", displayOrder = 1)
                insertMockedEcosystemPartnerCategory(catC, eventId = eventId, name = "Gamma", displayOrder = null)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/ecosystem-partner-categories") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(3, body.size)
        assertEquals("Beta", body[0].jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("Alpha", body[1].jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("Gamma", body[2].jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET public returns categories without authentication`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catA = UUID.randomUUID()
        val catB = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catA, eventId = eventId, name = "Zeta", displayOrder = 5)
                insertMockedEcosystemPartnerCategory(catB, eventId = eventId, name = "Alpha", displayOrder = 1)
            }
        }

        val response = client.get("/events/$eventId/ecosystem-partner-categories")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
        assertEquals("Alpha", body[0].jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("Zeta", body[1].jsonObject["name"]!!.jsonPrimitive.content)
    }
}
