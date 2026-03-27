package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.domain.PartnershipListMetadata
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract tests for GET /orgs/{orgSlug}/events/{eventSlug}/partnerships with filter[declined] parameter.
 * Tests HTTP contract: status codes, request/response schemas, and parameter validation.
 *
 * Per constitution Section II: Contract tests validate API schema in infrastructure.api package.
 */
class PartnershipListDeclinedFilterRouteGetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET without declined filter returns 200 and excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.items.size)
    }

    @Test
    fun `GET with declined filter=false returns 200 and excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=false") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.items.size)
    }

    @Test
    fun `GET with declined filter=true returns 200 and includes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.items.size)
    }

    @Test
    fun `GET with invalid declined filter value returns 400`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=maybe") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET without auth returns 401`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=true")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET with declined filter=true returns empty when no declined exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(0, body.items.size)
    }
}
