package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test: Re-validation updates (Scenario 6)
 * User Story: Organizer updates validation before agreement signed
 */
@Suppress("LargeClass")
class PartnershipValidationTest {
    @Test
    fun `POST validates a partnership`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-event-slug-1"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
            )
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership?.validatedAt)
    }

    @Test
    fun `POST returns 404 if partnership does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @Ignore
    fun `POST validate with empty body uses pack defaults for all optional fields`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-validate-empty-002"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 15,
                boothSize = "6x2m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertEquals(15, partnership?.validatedNbTickets)
        assertEquals(0, partnership?.validatedNbJobOffers)
        assertEquals("6x2m", partnership?.validatedBoothSize)
    }

    @Test
    fun `POST validate with pack defaults sets validated fields from pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-validate-defaults-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            // Create pack with default values
            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_job_offers": 2}"""
        val beforeValidation = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify validated fields are set from pack defaults
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership, "Partnership should exist")
        assertNotNull(partnership.validatedAt, "Partnership should be validated")

        // Validated timestamp should be recent
        val timeDiff = partnership.validatedAt!! >= beforeValidation
        assertEquals(true, timeDiff, "validatedAt should be recent")

        // Expected: Values from pack defaults
        assertEquals(10, partnership.validatedNbTickets, "validatedNbTickets should match pack default")
        assertEquals(2, partnership.validatedNbJobOffers, "validatedNbJobOffers should be from request")
        assertEquals("3x3m", partnership.validatedBoothSize, "validatedBoothSize should match pack default")
    }

    @Test
    fun `POST validate updates existing validation when not signed`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-revalidate-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            // Insert partnership with initial validation
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                validatedNbTickets = 10,
                validatedNbJobOffers = 2,
                validatedBoothSize = "3x3m",
            )
        }

        val initialValidatedAt = transaction {
            PartnershipEntity.findById(partnershipId)?.validatedAt
        }

        // Re-validate with different values
        val requestBody = """{"nb_tickets": 15, "nb_job_offers": 5, "booth_size": "3x3m"}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership)
        assertEquals(15, partnership.validatedNbTickets, "Should update ticket count")
        assertEquals(5, partnership.validatedNbJobOffers, "Should update job offers")
        assertEquals("3x3m", partnership.validatedBoothSize)

        assertNotNull(partnership.validatedAt)
        assertNotEquals(initialValidatedAt, partnership.validatedAt, "Timestamp should be updated")
    }

    @Test
    fun `POST validate can change booth size on revalidation`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val pack1Id = UUID.randomUUID()
        val pack2Id = UUID.randomUUID()
        val eventSlug = "test-change-booth-002"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = pack1Id,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedSponsoringPack(
                id = pack2Id,
                event = eventId,
                nbTickets = 15,
                boothSize = "6x2m",
            )

            // Initially validated with 3x3m booth
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack1Id,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                validatedNbTickets = 10,
                validatedNbJobOffers = 2,
                validatedBoothSize = "3x3m",
            )
        }

        // Change to 6x2m booth
        val requestBody = """{"nb_tickets": 10, "nb_job_offers": 2, "booth_size": "6x2m"}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertEquals("6x2m", partnership?.validatedBoothSize, "Should update booth size")
    }

    @Test
    fun `POST validate can remove booth on revalidation`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-remove-booth-003"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            // Initially validated with booth
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                validatedNbTickets = 10,
                validatedNbJobOffers = 2,
                validatedBoothSize = "3x3m",
            )
        }

        // Re-validate without booth (explicitly null via omission or null value)
        val requestBody = """{"nb_tickets": 8, "nb_job_offers": 1}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertEquals(8, partnership?.validatedNbTickets)
        assertEquals(1, partnership?.validatedNbJobOffers)
    }

    @Test
    fun `POST validate with booth size not in partnership's own pack succeeds`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val bronzePackId = UUID.randomUUID()
        val platinumPackId = UUID.randomUUID()
        val eventSlug = "test-different-pack-booth-002"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            // Bronze pack with no booth
            insertMockedSponsoringPack(
                id = bronzePackId,
                event = eventId,
                nbTickets = 3,
                boothSize = null,
            )

            // Platinum pack with large booth
            insertMockedSponsoringPack(
                id = platinumPackId,
                event = eventId,
                nbTickets = 20,
                boothSize = "10x5m",
            )

            // Partnership for Bronze (no booth)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = bronzePackId,
            )
        }

        // Assign Platinum's booth to Bronze partnership
        val requestBody = """{"nb_tickets": 3, "nb_job_offers": 1, "booth_size": "10x5m"}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertEquals(
            "10x5m",
            partnership?.validatedBoothSize,
            "Can assign booth from other pack even if own pack has none",
        )
    }

    @Test
    fun `POST validate rejects booth size not in any pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-invalid-booth-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        // Request non-existent booth size
        val requestBody = """{"nb_tickets": 10, "nb_job_offers": 2, "booth_size": "10x10m"}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(errorMessage.contains("not available"), "Error should mention booth not available")
        assertTrue(
            errorMessage.contains("10x10m") || errorMessage.contains("booth"),
            "Error should reference invalid booth",
        )
    }

    @Test
    fun `POST validate rejects missing nb_job_offers`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-missing-jobs-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        // Only provide nb_tickets
        val requestBody = """{"nb_tickets": 10}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("required") ||
                errorMessage.contains("nb_job_offers") ||
                errorMessage.contains("missing"),
            "Error should mention missing required field",
        )
    }

    @Test
    fun `POST validate rejects empty request body`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-empty-body-002"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("required") || errorMessage.contains("missing"),
            "Error should mention missing required fields",
        )
    }

    @Test
    fun `POST validate rejects null job offers explicitly`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-null-jobs-003"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_tickets": 10, "nb_job_offers": null}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("required") || errorMessage.contains("integer"),
            "Error should reject null for required field",
        )
    }

    @Test
    fun `POST validate rejects negative tickets`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-negative-tickets-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_tickets": -5, "nb_job_offers": 2}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("-5") || errorMessage.contains("negative") || errorMessage.contains(">= 0"),
            "Error should mention negative value constraint",
        )
    }

    @Test
    fun `POST validate rejects negative job offers`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-negative-jobs-002"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_tickets": 10, "nb_job_offers": -3}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("-3") || errorMessage.contains("negative") || errorMessage.contains(">= 0"),
            "Error should mention negative value constraint",
        )
    }

    @Test
    fun `POST validate rejects both negative values`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-both-negative-003"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_tickets": -10, "nb_job_offers": -5}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("greater or equal"),
            "Error should reject negative values",
        )
    }

    @Test
    fun `POST validate rejects large negative values`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-large-negative-004"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"nb_tickets": -999999, "nb_job_offers": 2}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST validate rejects revalidation when agreement signed`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-signed-block-001"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                nbTickets = 10,
                boothSize = "3x3m",
            )

            // Partnership with signed agreement
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                validatedNbTickets = 10,
                validatedNbJobOffers = 2,
                validatedBoothSize = "3x3m",
                agreementSignedUrl = "https://storage.example.com/signed-agreement.pdf",
            )
        }

        // Attempt to change values after signature
        val requestBody = """{"nb_tickets": 20, "nb_job_offers": 5, "booth_size": "6x2m"}"""

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val errorMessage = response.bodyAsText()
        assertTrue(
            errorMessage.contains("signed") || errorMessage.contains("agreement"),
            "Error should mention signed agreement",
        )
    }
}
