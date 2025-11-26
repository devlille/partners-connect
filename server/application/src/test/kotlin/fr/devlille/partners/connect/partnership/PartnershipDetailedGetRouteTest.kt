package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.DetailedPartnershipResponse
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("LongMethod")
class PartnershipDetailedGetRouteTest {
    @Test
    fun `GET detailed partnership returns 200 with complete partnership information`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            // Setup test data using existing mock factories
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(
                id = eventId,
                slug = eventSlug,
                startTime = "2025-06-13T18:00:00",
                endTime = "2025-06-14T18:00:00",
                submissionStartTime = "2025-01-01T00:00:00",
                submissionEndTime = "2025-03-01T23:59:59",
                address = "Lille Grand Palais, Lille, France",
                contactEmail = "contact@devlille.fr",
                contactPhone = "+33 6 12 34 56 78",
                organisation = org,
            )
            insertMockedCompany(id = companyId)
            val pack = insertMockedSponsoringPack(packId, eventId, name = "Gold Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                contactName = "John Smith",
                contactRole = "Partnership Manager",
                phone = "+33 1 23 45 67 89",
                language = "fr",
                selectedPackId = pack.id.value,
                validatedAt = LocalDateTime.parse("2025-01-15T10:00:00"),
                agreementUrl = "https://example.com/agreement.pdf",
                agreementSignedUrl = "https://example.com/signed-agreement.pdf",
            )
            // Add billing information for PAID status
            insertMockedBilling(
                eventId = eventId,
                partnershipId = partnershipId,
                status = InvoiceStatus.PAID,
            )
        }

        // This test MUST FAIL before implementation
        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        // Expected behavior (will fail until implemented):
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        // Validate the JSON structure matches the contract
        assertTrue(responseBody.contains("partnership"))
        assertTrue(responseBody.contains("company"))
        assertTrue(responseBody.contains("event"))
        assertTrue(responseBody.contains("organisation"))
        assertTrue(responseBody.contains("process_status"))
    }

    @Test
    fun `GET detailed partnership returns 404 for non-existent partnership`() = testApplication {
        val eventSlug = "test-event"
        val nonExistentId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        // This test MUST FAIL before implementation
        val response = client.get("/events/$eventSlug/partnerships/$nonExistentId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET detailed partnership returns 400 for invalid partnership ID`() = testApplication {
        val eventSlug = "test-event"
        val invalidId = "invalid-uuid"

        application {
            moduleMocked()
        }

        // This test MUST FAIL before implementation
        val response = client.get("/events/$eventSlug/partnerships/$invalidId")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET detailed partnership returns 404 for mismatched event-partnership association`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val wrongEventSlug = "wrong-event"

        application {
            moduleMocked()

            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(id = eventId, organisation = org, slug = "correct-event")
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        // This test MUST FAIL before implementation
        val response = client.get("/events/$wrongEventSlug/partnerships/$partnershipId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET detailed partnership with optional options`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            // Setup test data using existing mock factories
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(
                id = eventId,
                slug = eventSlug,
                startTime = "2025-06-13T18:00:00",
                endTime = "2025-06-14T18:00:00",
                submissionStartTime = "2025-01-01T00:00:00",
                submissionEndTime = "2025-03-01T23:59:59",
                address = "Lille Grand Palais, Lille, France",
                contactEmail = "contact@devlille.fr",
                contactPhone = "+33 6 12 34 56 78",
                organisation = org,
            )
            insertMockedCompany(id = companyId)
            val pack = insertMockedSponsoringPack(packId, eventId, name = "Gold Pack")
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedOptionTranslation(
                optionId = optionId,
                language = "fr",
                name = "Option de Test",
                description = "Description de l'option de test",
            )
            insertMockedPackOptions(packId = packId, optionId = optionId, required = false)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                contactName = "John Smith",
                contactRole = "Partnership Manager",
                phone = "+33 1 23 45 67 89",
                language = "fr",
                selectedPackId = pack.id.value,
                validatedAt = LocalDateTime.parse("2025-01-15T10:00:00"),
                agreementUrl = "https://example.com/agreement.pdf",
                agreementSignedUrl = "https://example.com/signed-agreement.pdf",
            )
            insertMockedOptionPartnership(
                partnershipId = partnershipId,
                packId = packId,
                optionId = optionId,
            )
            insertMockedBilling(
                eventId = eventId,
                partnershipId = partnershipId,
                status = InvoiceStatus.PAID,
            )
        }

        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val details = Json.decodeFromString(DetailedPartnershipResponse.serializer(), response.bodyAsText())
        assertNotNull(details.partnership.validatedPack)
        assertEquals(optionId.toString(), details.partnership.validatedPack.options.first().id)
    }
}
