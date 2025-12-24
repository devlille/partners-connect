package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertNotNull

/**
 * Contract test for partnership organiser assignment endpoints.
 * Tests JSON schema validation for assign_organiser_request.schema.json
 * and partnership_organiser_response.schema.json.
 * Validates that endpoints correctly handle valid and invalid requests according to schemas.
 */
class PartnershipOrganiserAttachmentRoutePostTest {
    @Test
    fun `POST assigns organiser with valid request schema`() = testApplication {
        val userId = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val organiserEmail = "$userId2@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedUser(id = userId2, email = organiserEmail)
                insertMockedOrgaPermission(orgId, userId = userId2)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"email":"$organiserEmail"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["partnership_id"]?.jsonPrimitive?.content)
        assertNotNull(responseBody["organiser"])
    }

    @Test
    fun `POST rejects missing email field`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST rejects invalid email format`() = testApplication {
        val userId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{ "email":"not-an-email"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST rejects to assign organiser to non-existent partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val userEmail = "$userId2@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedUser(id = userId2, email = userEmail)
            }
        }

        val requestBody = """{"email": "$userEmail"}"""
        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST rejects to assign non-existent user as organiser`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(id = companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            }
        }

        val requestBody = """{ "email": "nonexistent@example.com"}"""

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST rejects to assign user without organisation membership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val userEmail = "$userId2@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(id = companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
                insertMockedUser(id = userId2, email = userEmail)
            }
        }

        val requestBody = """{ "email": "$userEmail"}"""
        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST rejects to assign user without edit permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val userEmail = "$userId2@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(id = companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
                insertMockedUser(id = userId2, email = userEmail)
                insertMockedOrgaPermission(orgId = orgId, userId = userId2, canEdit = false)
            }
        }

        val requestBody = """{"email": "$userEmail"}"""

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
