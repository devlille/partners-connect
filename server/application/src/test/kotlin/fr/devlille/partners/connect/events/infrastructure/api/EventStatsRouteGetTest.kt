package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventStatsRouteGetTest {
    @Test
    fun `GET returns 200 with empty partners list when event has no partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/stats") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partners = body["partners"]!!.jsonArray
        assertEquals(0, partners.size)
    }

    @Test
    fun `GET returns 200 with one partner row per non-declined partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val activePartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedPartnership(
                    id = activePartnershipId,
                    eventId = eventId,
                    companyId = activeCompanyId,
                )
                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    declinedAt = kotlinx.datetime.LocalDateTime.parse("2024-01-01T00:00:00"),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/stats") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partners = body["partners"]!!.jsonArray
        assertEquals(1, partners.size)
    }

    @Test
    fun `GET returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/events/unknown-event/stats") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 401 when no authorization header is provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/stats")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 401 when user has no permission on the organisation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/stats") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
