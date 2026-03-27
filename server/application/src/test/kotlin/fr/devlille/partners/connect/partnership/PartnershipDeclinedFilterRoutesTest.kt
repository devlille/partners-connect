package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.FilterType
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for filter[declined] parameter on:
 * - GET /orgs/{orgSlug}/events/{eventSlug}/partnerships (User Story 1)
 * - POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email (User Story 2)
 *
 * Tests end-to-end business logic for filtering partnerships by declined status.
 * Covers the intentional breaking change: declined partnerships are excluded by default.
 *
 * Per constitution Section II: Integration tests validate complete workflows in root package.
 */
@Suppress("LargeClass")
class PartnershipDeclinedFilterRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    // ============================================================================================
    // User Story 1: View Declined Partnerships in Partnership List
    // ============================================================================================

    @Test
    @Suppress("LongMethod")
    fun `GET default filter excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val activePartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)

                insertMockedPartnership(
                    id = activePartnershipId,
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                    declinedAt = null,
                )
                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = now,
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
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertEquals(activePartnershipId.toString(), body.items.first().id)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET with declined filter=false explicitly excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val activePartnershipId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)

                insertMockedPartnership(
                    id = activePartnershipId,
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = now,
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
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertEquals(activePartnershipId.toString(), body.items.first().id)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET with declined filter=true includes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
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
                    declinedAt = null,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = now,
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
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET declined filter combines with other filters using AND logic`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val company3Id = UUID.randomUUID()
        val validatedAndDeclined = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedCompany(company3Id)
                insertMockedSponsoringPack(packId, eventId)

                // Active + validated
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    validatedAt = now,
                    declinedAt = null,
                )
                // Active + not validated
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    validatedAt = null,
                    declinedAt = null,
                )
                // Declined + validated (matches both filters)
                insertMockedPartnership(
                    id = validatedAndDeclined,
                    eventId = eventId,
                    companyId = company3Id,
                    selectedPackId = packId,
                    validatedAt = now,
                    declinedAt = now,
                )
            }
        }

        // declined=true means "only declined", validated=true means "only validated"
        // Result: only partnerships where declinedAt IS NOT NULL AND validatedAt IS NOT NULL
        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[declined]=true&filter[validated]=true",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        // Returns only the declined+validated partnership
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertTrue(body.items.any { it.id == validatedAndDeclined.toString() })
    }

    @Test
    @Suppress("LongMethod")
    fun `GET without filter returns empty list when all partnerships are declined`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
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
                    declinedAt = now,
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    declinedAt = now,
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
        assertEquals(0, body.total)
        assertTrue(body.items.isEmpty())
    }

    @Test
    fun `GET response metadata includes declined filter entry`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        val metadata = body.metadata
        assertNotNull(metadata)
        val declinedFilter = metadata.filters.find { it.name == "declined" }
        assertNotNull(declinedFilter, "metadata.filters should include 'declined' entry")
        assertEquals(FilterType.BOOLEAN, declinedFilter.type)
        assertTrue(declinedFilter.values.isNullOrEmpty(), "declined filter should have no fixed values")
    }

    // ============================================================================================
    // User Story 2: Control Declined Partnerships in Bulk Email Sending
    // ============================================================================================

    @Test
    @Suppress("LongMethod")
    fun `POST email default filter excludes declined partnerships from recipients and returns 404 when all declined`() =
        testApplication {
            val userId = UUID.randomUUID()
            val orgId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val packId = UUID.randomUUID()
            val company1Id = UUID.randomUUID()
            val company2Id = UUID.randomUUID()
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            application {
                moduleSharedDb(userId = userId)
                transaction {
                    insertMockedUser(userId)
                    insertMockedOrganisationEntity(orgId)
                    insertMockedOrgaPermission(orgId, userId = userId)
                    insertMockedFutureEvent(eventId, orgId = orgId)
                    insertMockedCompany(company1Id)
                    insertMockedCompany(company2Id)
                    insertMockedSponsoringPack(packId, eventId)

                    // Both partnerships are declined
                    insertMockedPartnership(
                        id = UUID.randomUUID(),
                        eventId = eventId,
                        companyId = company1Id,
                        selectedPackId = packId,
                        declinedAt = now,
                    )
                    insertMockedPartnership(
                        id = UUID.randomUUID(),
                        eventId = eventId,
                        companyId = company2Id,
                        selectedPackId = packId,
                        declinedAt = now,
                    )
                }
            }

            // POST email without filter[declined] — declined partnerships excluded, leaving zero recipients
            val response = client.post("/orgs/$orgId/events/$eventId/partnerships/email") {
                header(HttpHeaders.Authorization, "Bearer valid")
                contentType(ContentType.Application.Json)
                setBody("""{"subject":"Test Subject","body":"<p>Hello</p>"}""")
            }

            // Route throws NotFoundException when no recipients match after applying the declined filter
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
}
