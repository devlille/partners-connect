package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EcosystemPartnerCategoryRoutePutTest {
    @Test
    fun `PUT updates the category name`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catId, eventId = eventId, name = "Media")
            }
        }

        val body = UpdateEcosystemPartnerCategory(name = "Press")
        val response = client.put("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(UpdateEcosystemPartnerCategory.serializer(), body))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Press", json["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `PUT returns 409 when renaming to an existing name`() = testApplication {
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
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catA, eventId = eventId, name = "Media")
                insertMockedEcosystemPartnerCategory(catB, eventId = eventId, name = "Press")
            }
        }

        val body = UpdateEcosystemPartnerCategory(name = "Press")
        val response = client.put("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catA") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(UpdateEcosystemPartnerCategory.serializer(), body))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `PUT returns 404 when category belongs to a different event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val otherEventId = UUID.randomUUID()
        val catId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedFutureEvent(otherEventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catId, eventId = otherEventId, name = "Media")
            }
        }

        val body = UpdateEcosystemPartnerCategory(name = "Press")
        val response = client.put("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(UpdateEcosystemPartnerCategory.serializer(), body))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
