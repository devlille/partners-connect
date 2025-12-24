package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnershipListRouteGetTest {
    @Test
    fun `GET returns one partnership when one exists`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

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

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
    }

    @Test
    fun `GET returns partnerships for event without filters`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

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
                    contactName = "John Doe",
                    contactRole = "Manager",
                )
                insertMockedPartnership(
                    id = UUID.randomUUID(),
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                    contactRole = "Director",
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(2, partnerships.size)

        // Verify first partnership
        val partnership1 = partnerships.find { it.contact.displayName == "John Doe" }!!
        assertEquals("Manager", partnership1.contact.role)
        assertEquals("$company1Id", partnership1.companyName)
        assertEquals("$packId", partnership1.selectedPackName)
        assertNull(partnership1.suggestedPackName)
        assertEquals("en", partnership1.language)

        // Verify second partnership
        val partnership2 = partnerships.find { it.contact.displayName == "Jane Smith" }!!
        assertEquals("Director", partnership2.contact.role)
        assertEquals("$company2Id", partnership2.companyName)
        assertEquals("$packId", partnership2.selectedPackName)
    }

    @Test
    fun `GET filter by pack_id returns only matching partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId1 = UUID.randomUUID()
        val packId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedSponsoringPack(packId1, eventId)
                insertMockedSponsoringPack(packId2, eventId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId1,
                    contactName = "John Doe",
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId2,
                    contactName = "Jane Smith",
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[pack_id]=$packId1") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
        assertEquals("$packId1", partnerships[0].selectedPackName)
    }

    @Test
    fun `GET filter by validated=true returns only validated partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

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
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    contactName = "John Doe",
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                    // No validatedAt
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[validated]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by validated=false returns only non-validated partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

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
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    contactName = "John Doe",
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                    // No validatedAt
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[validated]=false") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("Jane Smith", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by suggestion=true returns only partnerships with suggestions`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val suggestionPackId = UUID.randomUUID()

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
                insertMockedSponsoringPack(suggestionPackId, eventId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    suggestionPackId = suggestionPackId,
                    contactName = "John Doe",
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                    // No suggestion
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[suggestion]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        val partnership = partnerships[0]
        assertEquals("John Doe", partnership.contact.displayName)
        assertEquals("$suggestionPackId", partnership.suggestedPackName)
    }

    @Test
    fun `GET filter by paid=true returns only paid partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

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
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    contactName = "John Doe",
                )
                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                )
                insertMockedBilling(eventId, partnership1Id, status = InvoiceStatus.PAID)
                insertMockedBilling(eventId, partnership2Id, status = InvoiceStatus.PENDING)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[paid]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET filter by agreement-generated=true returns only partnerships with agreements`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

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
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    contactName = "John Doe",
                    agreementUrl = "https://example.com/agreement.pdf",
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    contactName = "Jane Smith",
                    // No agreement URL
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[agreement-generated]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
    }

    @Test
    fun `GET with combined filters returns intersection`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId1 = UUID.randomUUID()
        val packId2 = UUID.randomUUID()
        val suggestionPackId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedSponsoringPack(packId1, eventId)
                insertMockedSponsoringPack(packId2, eventId)
                insertMockedSponsoringPack(suggestionPackId, eventId)

                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId1,
                    suggestionPackId = suggestionPackId,
                    contactName = "John Doe",
                )

                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId2,
                    contactName = "Jane Smith",
                    // No suggestion
                )
            }
        }

        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[pack_id]=$packId1&filter[suggestion]=true",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, partnerships.size)
        assertEquals("John Doe", partnerships[0].contact.displayName)
        assertEquals("$packId1", partnerships[0].selectedPackName)
        assertEquals("$suggestionPackId", partnerships[0].suggestedPackName)
    }

    @Test
    fun `GET returns empty array when no partnerships match filters`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val nonExistentPackId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[pack_id]=$nonExistentPackId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = Json.decodeFromString<List<PartnershipItem>>(response.bodyAsText())
        assertTrue(partnerships.isEmpty())
    }

    @Test
    fun `GET returns 401 when no authorization header`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 401 when user lacks organisation permissions`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
