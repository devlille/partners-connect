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
import kotlin.test.assertNotNull

class PartnershipListMetadataRouteGetTest {
    @Test
    fun `GET returns metadata with pack counts`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val pack1Id = UUID.randomUUID()
        val pack2Id = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val company3Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedCompany(company3Id)
                insertMockedSponsoringPack(pack1Id, eventId, name = "Gold")
                insertMockedSponsoringPack(pack2Id, eventId, name = "Silver")
                // 2 partnerships on Gold pack
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = pack1Id,
                )
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = pack1Id,
                )
                // 1 partnership on Silver pack
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company3Id,
                    selectedPackId = pack2Id,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        val metadata = result.metadata
        assertNotNull(metadata)
        assertEquals(3, result.items.size)

        val goldCount = metadata.packCounts.find { it.packName == "Gold" }
        assertNotNull(goldCount)
        assertEquals(pack1Id.toString(), goldCount.packId)
        assertEquals(2, goldCount.count)

        val silverCount = metadata.packCounts.find { it.packName == "Silver" }
        assertNotNull(silverCount)
        assertEquals(pack2Id.toString(), silverCount.packId)
        assertEquals(1, silverCount.count)
    }

    @Test
    fun `GET excludes declined partnerships from pack counts`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId, name = "Gold")
                // Active partnership
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                )
                // Declined partnership
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        val metadata = result.metadata
        assertNotNull(metadata)

        val goldCount = metadata.packCounts.find { it.packName == "Gold" }
        assertNotNull(goldCount)
        assertEquals(1, goldCount.count)
    }

    @Test
    fun `GET returns pack with zero count when no partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId, name = "Platinum")
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        val metadata = result.metadata
        assertNotNull(metadata)
        assertEquals(1, metadata.packCounts.size)

        val platinumCount = metadata.packCounts.first()
        assertEquals(packId.toString(), platinumCount.packId)
        assertEquals("Platinum", platinumCount.packName)
        assertEquals(0, platinumCount.count)
    }
}
