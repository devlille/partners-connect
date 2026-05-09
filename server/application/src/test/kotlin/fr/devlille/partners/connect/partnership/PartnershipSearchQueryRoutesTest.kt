package fr.devlille.partners.connect.partnership

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
import kotlin.test.assertTrue

/**
 * Tests for GET /orgs/{orgSlug}/events/{eventSlug}/partnerships?query=...
 * Search is case-insensitive substring match across company name and partnership contact name.
 */
class PartnershipSearchQueryRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `query matches partnerships by company name (case-insensitive substring)`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val matchingCompanyId = UUID.randomUUID()
        val otherCompanyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(matchingCompanyId, name = "Acme Corporation")
                insertMockedCompany(otherCompanyId, name = "Globex Industries")
                val pack = insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = matchingCompanyId,
                    selectedPackId = pack.id.value,
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = otherCompanyId,
                    selectedPackId = pack.id.value,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?query=acme") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertEquals("Acme Corporation", body.items.first().companyName)
    }

    @Test
    fun `query matches partnerships by contact display name`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id, name = "Company One")
                insertMockedCompany(company2Id, name = "Company Two")
                val pack = insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack.id.value,
                    contactName = "Alice Martin",
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack.id.value,
                    contactName = "Bob Durand",
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?query=alice") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.total)
        assertEquals("Alice Martin", body.items.first().contact.displayName)
    }

    @Test
    fun `blank query returns all partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id, name = "Acme Corporation")
                insertMockedCompany(company2Id, name = "Globex Industries")
                val pack = insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(eventId = eventId, companyId = company1Id, selectedPackId = pack.id.value)
                insertMockedPartnership(eventId = eventId, companyId = company2Id, selectedPackId = pack.id.value)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?query=%20%20") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(2, body.total)
    }

    @Test
    fun `query with no match returns empty list`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "Acme Corporation")
                val pack = insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(eventId = eventId, companyId = companyId, selectedPackId = pack.id.value)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?query=nonexistent") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(0, body.total)
        assertTrue(body.items.isEmpty())
    }

    @Test
    fun `query combines with other filters using AND logic`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id, name = "Acme Corporation")
                insertMockedCompany(company2Id, name = "Acme Subsidiary")
                val pack = insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack.id.value,
                    validatedAt = now,
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack.id.value,
                    validatedAt = null,
                )
            }
        }

        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?query=acme&filter[validated]=true",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.total)
        assertEquals("Acme Corporation", body.items.first().companyName)
    }
}
