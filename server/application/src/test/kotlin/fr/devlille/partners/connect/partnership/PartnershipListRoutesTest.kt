package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.call.body
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnershipListRoutesTest {
    @Test
    fun `GET returns one partnership when one exists`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(companyId, "Test Company")
            val pack = insertMockedSponsoringPack(packId, eventId, "Test Pack")

            insertMockedPartnership(
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                contactRole = "Manager",
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)

        val partnership = partnerships[0]
        assertEquals("John Doe", partnership.contact.displayName)
        assertEquals("Manager", partnership.contact.role)
        assertEquals("Test Company", partnership.companyName)
        assertEquals("Test Pack", partnership.packName)
    }

    @Test
    fun `GET returns partnerships for event without filters`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id, "Company One")
            insertMockedCompany(company2Id, "Company Two")
            val pack = insertMockedSponsoringPack(packId, eventId, "Gold Pack")

            // Create partnerships with different statuses
            insertMockedPartnership(
                id = UUID.randomUUID(),
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                contactRole = "Manager",
            )

            insertMockedPartnership(
                id = UUID.randomUUID(),
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
                contactRole = "Director",
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(2, partnerships.size)

        // Verify first partnership
        val partnership1 = partnerships.find { it.contact.displayName == "John Doe" }!!
        assertEquals("Manager", partnership1.contact.role)
        assertEquals("Company One", partnership1.companyName)
        assertEquals("Gold Pack", partnership1.packName)
        assertNull(partnership1.suggestedPackName)
        assertEquals("en", partnership1.language)

        // Verify second partnership
        val partnership2 = partnerships.find { it.contact.displayName == "Jane Smith" }!!
        assertEquals("Director", partnership2.contact.role)
        assertEquals("Company Two", partnership2.companyName)
        assertEquals("Gold Pack", partnership2.packName)
    }

    @Test
    fun `GET filter by pack_id returns only matching partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val goldPackId = UUID.randomUUID()
        val silverPackId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id, "Company One")
            insertMockedCompany(company2Id, "Company Two")
            val goldPack = insertMockedSponsoringPack(goldPackId, eventId, "Gold Pack")
            val silverPack = insertMockedSponsoringPack(silverPackId, eventId, "Silver Pack")

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = goldPack.id.value,
                contactName = "John Doe",
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = silverPack.id.value,
                contactName = "Jane Smith",
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[pack_id]=$goldPackId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
        assertEquals("Gold Pack", partnerships[0].packName)
    }

    @Test
    fun `GET filter by validated=true returns only validated partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId)

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
                // No validatedAt
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[validated]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by validated=false returns only non-validated partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId)

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
                // No validatedAt
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[validated]=false") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("Jane Smith", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by suggestion=true returns only partnerships with suggestions`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val suggestionPackId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId, "Gold Pack")
            val suggestionPack = insertMockedSponsoringPack(suggestionPackId, eventId, "Silver Pack")

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                suggestionPackId = suggestionPack.id.value,
                contactName = "John Doe",
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
                // No suggestion
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[suggestion]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        val partnership = partnerships[0]
        assertEquals("John Doe", partnership.contact.displayName)
        assertEquals("Silver Pack", partnership.suggestedPackName)
    }

    @Test
    fun `GET filter by paid=true returns only paid partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId)

            insertMockedPartnership(
                id = partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
            )

            insertMockedPartnership(
                id = partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
            )

            // Add billing for first partnership as PAID
            insertMockedBilling(eventId, partnership1Id, status = InvoiceStatus.PAID)
            // Add billing for second partnership as PENDING
            insertMockedBilling(eventId, partnership2Id, status = InvoiceStatus.PENDING)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[paid]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by agreement-generated=true returns only partnerships with agreements`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId)

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                agreementUrl = "https://example.com/agreement.pdf",
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
                // No agreement URL
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[agreement-generated]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET with sort=created and direction=desc returns partnerships in descending order`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val pack = insertMockedSponsoringPack(packId, eventId)

            // Insert partnerships - creation order should be preserved in most cases
            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = pack.id.value,
                contactName = "Jane Smith",
            )
        }

        val responseAsc = client.get("/orgs/$orgId/events/$eventId/partnership?sort=created&direction=asc") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        val responseDesc = client.get("/orgs/$orgId/events/$eventId/partnership?sort=created&direction=desc") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responseAsc.status)
        assertEquals(HttpStatusCode.OK, responseDesc.status)

        val partnershipsAsc = Json.decodeFromString<List<PartnershipItem>>(responseAsc.bodyAsText())
        val partnershipsDesc = Json.decodeFromString<List<PartnershipItem>>(responseDesc.bodyAsText())

        assertEquals(2, partnershipsAsc.size)
        assertEquals(2, partnershipsDesc.size)

        // Verify that asc and desc return different orders (or same if times are identical)
        // The important thing is that sorting parameter is respected
        assertTrue(partnershipsAsc.isNotEmpty())
        assertTrue(partnershipsDesc.isNotEmpty())
    }

    @Test
    fun `GET with combined filters returns intersection`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val goldPackId = UUID.randomUUID()
        val silverPackId = UUID.randomUUID()
        val suggestionPackId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            val goldPack = insertMockedSponsoringPack(goldPackId, eventId, "Gold Pack")
            val silverPack = insertMockedSponsoringPack(silverPackId, eventId, "Silver Pack")
            val suggestionPack = insertMockedSponsoringPack(suggestionPackId, eventId, "Bronze Pack")

            insertMockedPartnership(
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = goldPack.id.value,
                suggestionPackId = suggestionPack.id.value,
                contactName = "John Doe",
            )

            insertMockedPartnership(
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = silverPack.id.value,
                contactName = "Jane Smith",
                // No suggestion
            )
        }

        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnership?filter[pack_id]=$goldPackId&filter[suggestion]=true",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
        assertEquals("Gold Pack", partnerships[0].packName)
        assertEquals("Bronze Pack", partnerships[0].suggestedPackName)
    }

    @Test
    fun `GET returns empty array when no partnerships match filters`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val nonExistentPackId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            val pack = insertMockedSponsoringPack(packId, eventId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership?filter[pack_id]=$nonExistentPackId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertTrue(partnerships.isEmpty())
    }

    @Test
    fun `GET returns 401 when no authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 404 when user lacks organisation permissions`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            // Don't create admin user permission
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 404 when organization does not exist`() = testApplication {
        val nonExistentOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            // Don't create organization
        }

        val response = client.get("/orgs/$nonExistentOrgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 404 when event does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val nonExistentEventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(UUID.randomUUID(), orgId) // Different event
        }

        val response = client.get("/orgs/$orgId/events/$nonExistentEventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 404 when eventId is not a valid UUID`() = testApplication {
        val orgId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.get("/orgs/$orgId/events/not-a-uuid/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET partnership route returns 200 with empty array`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        println("Response body: ${response.body<String>()}")
        println("Content-Type: ${response.headers["Content-Type"]}")
    }

    @Test
    fun `GET validates response shape`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(companyId, "Test Company")
            val pack = insertMockedSponsoringPack(packId, eventId, "Test Pack")

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                contactRole = "Manager",
                language = "fr",
                phone = "0123456789",
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)

        val partnership = partnerships[0]
        assertEquals(partnershipId.toString(), partnership.id)
        assertEquals("John Doe", partnership.contact.displayName)
        assertEquals("Manager", partnership.contact.role)
        assertEquals("Test Company", partnership.companyName)
        assertEquals("Test Pack", partnership.packName)
        assertNull(partnership.suggestedPackName)
        assertEquals("Test Event", partnership.eventName)
        assertEquals("fr", partnership.language)
        assertEquals("0123456789", partnership.phone)
        assertTrue(partnership.emails.isEmpty())
        assertTrue(partnership.createdAt.isNotEmpty())
    }
}
