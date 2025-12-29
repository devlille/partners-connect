package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for GET /orgs/{orgSlug}/events/{eventSlug}/partnerships with filter[organiser] parameter.
 * Tests end-to-end business logic for filtering partnerships by assigned organiser email.
 *
 * Per constitution Section II: Integration tests validate complete workflows in root package.
 */
class PartnershipOrganiserFilterRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod")
    fun `organiser filter returns only assigned partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val organiser1Id = UUID.randomUUID()
        val organiser2Id = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val company3Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()
        val partnership3Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                // Setup users
                insertMockedUser(userId)
                val organiser1 = insertMockedUser(organiser1Id, email = "organiser1@example.com")
                val organiser2 = insertMockedUser(organiser2Id, email = "organiser2@example.com")

                // Setup org and event
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedOrgaPermission(orgId, userId = organiser1Id)
                insertMockedOrgaPermission(orgId, userId = organiser2Id)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Setup companies and pack
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedCompany(company3Id)
                val pack = insertMockedSponsoringPack(packId, eventId)

                // Create partnerships with different organisers
                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser1.id.value,
                )
                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser1.id.value,
                )
                insertMockedPartnership(
                    id = partnership3Id,
                    eventId = eventId,
                    companyId = company3Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser2.id.value,
                )
            }
        }

        // Filter by organiser1
        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[organiser]=organiser1@example.com",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())

        // Should return only partnerships assigned to organiser1
        assertEquals(2, body.total)
        assertEquals(2, body.items.size)
        assertTrue(body.items.all { it.organiser?.email == "organiser1@example.com" })
    }

    @Test
    fun `filter excludes partnerships with no assigned organiser`() = testApplication {
        val userId = UUID.randomUUID()
        val organiserId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                // Setup users
                insertMockedUser(userId)
                val organiser = insertMockedUser(organiserId, email = "organiser-excludes@example.com")

                // Setup org and event
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedOrgaPermission(orgId, userId = organiserId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Setup companies and pack
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                val pack = insertMockedSponsoringPack(packId, eventId)

                // Create partnership WITH organiser
                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser.id.value,
                )

                // Create partnership WITHOUT organiser (null)
                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack.id.value,
                    organiserId = null,
                )
            }
        }

        // Filter by organiser
        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[organiser]=organiser-excludes@example.com",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())

        // Should return only partnership with organiser, exclude null organiser
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertEquals("organiser-excludes@example.com", body.items.first().organiser?.email)
    }

    @Test
    fun `filter combines with other filters using AND logic`() = testApplication {
        val userId = UUID.randomUUID()
        val organiserId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                // Setup users
                insertMockedUser(userId)
                val organiser = insertMockedUser(organiserId, email = "organiser-combines@example.com")

                // Setup org and event
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedOrgaPermission(orgId, userId = organiserId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Setup companies and pack
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                val pack = insertMockedSponsoringPack(packId, eventId)

                // Create validated partnership with organiser
                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser.id.value,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )

                // Create unvalidated partnership with same organiser
                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack.id.value,
                    organiserId = organiser.id.value,
                    validatedAt = null,
                )
            }
        }

        // Filter by organiser AND validated
        val url = "/orgs/$orgId/events/$eventId/partnerships" +
            "?filter[organiser]=organiser-combines@example.com&filter[validated]=true"
        val response = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())

        // Should return only validated partnership with organiser
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
        assertEquals("organiser-combines@example.com", body.items.first().organiser?.email)
        assertNotNull(body.items.first().validatedPackId)
    }
}
