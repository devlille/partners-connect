package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartnershipDetailIntegrationTest {
    @Suppress("LongMethod")
    @Test
    fun `integration test - partnership retrieval with full process status workflow`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            // Setup complete test scenario with all related entities
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(
                id = eventId,
                name = "DevLille 2025",
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

            insertMockedCompany(
                id = companyId,
                name = "Tech Solutions Inc",
                address = "456 Innovation Blvd",
                city = "Lyon",
                zipCode = "69000",
                country = "FR",
                siret = "98765432109876",
                vat = "FR98765432109",
                description = "Innovative technology solutions provider",
                siteUrl = "https://www.techsolutions.com",
            )

            val sponsoringPack = insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                name = "Platinum Sponsor",
            )

            // Create partnership in PAID status (full workflow completion)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                contactName = "Alice Johnson",
                contactRole = "Business Development Director",
                phone = "+33 4 56 78 90 12",
                language = "en",
                selectedPackId = sponsoringPack.id.value,
                suggestionSentAt = LocalDateTime.parse("2025-01-10T09:00:00"),
                suggestionApprovedAt = LocalDateTime.parse("2025-01-12T14:00:00"),
                agreementUrl = "https://storage.example.com/agreements/partnership-123.pdf",
                agreementSignedUrl = "https://storage.example.com/signed/partnership-123-signed.pdf",
                validatedAt = LocalDateTime.parse("2025-01-15T16:30:00"),
                communicationPublicationDate = LocalDateTime.parse("2025-02-01T08:00:00"),
                communicationSupportUrl = "https://storage.example.com/communication/partnership-123-assets.zip",
            )

            // Add billing information for PAID status
            insertMockedBilling(
                eventId = eventId,
                partnershipId = partnershipId,
                status = InvoiceStatus.PAID,
            )
        }

        // This integration test MUST FAIL before implementation
        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        // Verify successful response
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.headers["Content-Type"]?.contains("application/json") ?: false)

        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject

        // Validate complete response structure
        assertNotNull(json["partnership"])
        assertNotNull(json["company"])
        assertNotNull(json["event"])

        // Validate partnership details
        val partnershipData = json["partnership"]!!.jsonObject
        assertNotNull(partnershipData["process_status"]) // process_status is inside partnership
        assertEquals("Alice Johnson", partnershipData["contact_name"]!!.jsonPrimitive.content)
        assertEquals("Business Development Director", partnershipData["contact_role"]!!.jsonPrimitive.content)
        assertEquals("+33 4 56 78 90 12", partnershipData["phone"]!!.jsonPrimitive.content)
        assertEquals("en", partnershipData["language"]!!.jsonPrimitive.content)

        // Validate company information
        val companyData = json["company"]!!.jsonObject
        assertEquals("Tech Solutions Inc", companyData["name"]!!.jsonPrimitive.content)
        val headOffice = companyData["head_office"]!!.jsonObject
        assertEquals("456 Innovation Blvd", headOffice["address"]!!.jsonPrimitive.content)
        assertEquals("Lyon", headOffice["city"]!!.jsonPrimitive.content)
        assertEquals("69000", headOffice["zip_code"]!!.jsonPrimitive.content)
        assertEquals("FR", headOffice["country"]!!.jsonPrimitive.content)
        assertEquals("98765432109876", companyData["siret"]!!.jsonPrimitive.content)
        assertEquals("FR98765432109", companyData["vat"]!!.jsonPrimitive.content)
        assertEquals("Innovative technology solutions provider", companyData["description"]!!.jsonPrimitive.content)
        assertEquals("https://www.techsolutions.com", companyData["site_url"]!!.jsonPrimitive.content)

        // Validate event information
        val eventData = json["event"]!!.jsonObject
        assertEquals("DevLille 2025", eventData["name"]!!.jsonPrimitive.content)
        assertEquals(eventSlug, eventData["slug"]!!.jsonPrimitive.content)
        assertEquals("2025-06-13T18:00", eventData["start_time"]!!.jsonPrimitive.content)
        assertEquals("2025-06-14T18:00", eventData["end_time"]!!.jsonPrimitive.content)
        assertEquals("Lille Grand Palais, Lille, France", eventData["address"]!!.jsonPrimitive.content)

        val processStatus = partnershipData["process_status"]!!.jsonObject
        // Validate timestamp fields in process status
        assertNotNull(processStatus["suggestion_sent_at"])
        assertNotNull(processStatus["suggestion_approved_at"])
        assertNotNull(processStatus["validated_at"])
        assertNotNull(processStatus["agreement_url"])
        assertNotNull(processStatus["agreement_signed_url"])
        assertNotNull(processStatus["communication_publication_date"])
        assertNotNull(processStatus["communication_support_url"])
        assertEquals("PAID", processStatus["billing_status"]!!.jsonPrimitive.content)

        // Validate validated pack information
        assertNotNull(partnershipData["validated_pack"])
        val validatedPack = partnershipData["validated_pack"]!!.jsonObject
        assertEquals("Platinum Sponsor", validatedPack["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `integration test - partnership in SUGGESTION_SENT status`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(id = companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)

            // Partnership with only suggestion sent
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                suggestionPackId = pack.id.value,
                suggestionSentAt = LocalDateTime.parse("2025-01-10T09:00:00"),
                // No other workflow steps completed
            )
        }

        // This test MUST FAIL before implementation
        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partnershipData = json["partnership"]!!.jsonObject
        val processStatus = partnershipData["process_status"]!!.jsonObject

        // Validate that only suggestion_sent_at is populated, others are null
        assertNotNull(processStatus["suggestion_sent_at"])
        assertEquals(null, processStatus["suggestion_approved_at"]?.jsonPrimitive?.content)
        assertEquals(null, processStatus["validated_at"]?.jsonPrimitive?.content)
        assertEquals(null, processStatus["agreement_url"]?.jsonPrimitive?.content)
        assertEquals(null, processStatus["agreement_signed_url"]?.jsonPrimitive?.content)
        assertEquals(null, processStatus["communication_publication_date"]?.jsonPrimitive?.content)
        assertEquals(null, processStatus["billing_status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `integration test - partnership repositories orchestration`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            // Minimal setup to test repository orchestration
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        // This test MUST FAIL before implementation - tests multi-repository orchestration
        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Verify that data comes from different repositories:
        // - Partnership data from PartnershipRepository
        // - Company data from CompanyRepository
        // - Event data from EventRepository
        assertTrue(json.containsKey("partnership"))
        assertTrue(json.containsKey("company"))
        assertTrue(json.containsKey("event"))

        // Verify that process_status is inside partnership object
        val partnership = json["partnership"]?.jsonObject
        assertNotNull(partnership)
        assertTrue(partnership.containsKey("process_status"))
    }
}
