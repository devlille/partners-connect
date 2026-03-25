package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
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

@Suppress("LargeClass")
class PartnershipPaginationWithFiltersRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod")
    fun `GET pagination works with validated filter`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)

                // Create 5 validated partnerships
                repeat(5) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                        validatedAt = now,
                    )
                }
                // Create 3 non-validated partnerships
                repeat(3) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                        validatedAt = null,
                    )
                }
            }
        }

        val responsePage1 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[validated]=true&page=1&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage1.status)
        val page1 = json.decodeFromString<PaginatedResponse<PartnershipItem>>(responsePage1.bodyAsText())
        assertEquals(1, page1.page)
        assertEquals(2, page1.pageSize)
        assertEquals(5, page1.total)
        assertEquals(2, page1.items.size)

        val responsePage2 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[validated]=true&page=2&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage2.status)
        val page2 = json.decodeFromString<PaginatedResponse<PartnershipItem>>(responsePage2.bodyAsText())
        assertEquals(2, page2.page)
        assertEquals(2, page2.pageSize)
        assertEquals(5, page2.total)
        assertEquals(2, page2.items.size)

        // No overlap between pages
        val page1Ids = page1.items.map { it.id }
        val page2Ids = page2.items.map { it.id }
        assertEquals(0, page1Ids.intersect(page2Ids.toSet()).size)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET pagination works with paid filter`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)

                // Create 4 paid partnerships
                repeat(4) {
                    val companyId = UUID.randomUUID()
                    val partnershipId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        id = partnershipId,
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                    )
                    insertMockedBilling(
                        eventId = eventId,
                        partnershipId = partnershipId,
                        status = InvoiceStatus.PAID,
                    )
                }
                // Create 2 unpaid partnerships
                repeat(2) {
                    val companyId = UUID.randomUUID()
                    val partnershipId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        id = partnershipId,
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                    )
                    insertMockedBilling(
                        eventId = eventId,
                        partnershipId = partnershipId,
                        status = InvoiceStatus.PENDING,
                    )
                }
            }
        }

        val responsePage1 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[paid]=true&page=1&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage1.status)
        val page1 = json.decodeFromString<PaginatedResponse<PartnershipItem>>(responsePage1.bodyAsText())
        assertEquals(1, page1.page)
        assertEquals(2, page1.pageSize)
        assertEquals(4, page1.total)
        assertEquals(2, page1.items.size)

        val responsePage2 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[paid]=true&page=2&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage2.status)
        val page2 = json.decodeFromString<PaginatedResponse<PartnershipItem>>(responsePage2.bodyAsText())
        assertEquals(2, page2.page)
        assertEquals(2, page2.pageSize)
        assertEquals(4, page2.total)
        assertEquals(2, page2.items.size)

        // No overlap between pages
        val page1Ids = page1.items.map { it.id }
        val page2Ids = page2.items.map { it.id }
        assertEquals(0, page1Ids.intersect(page2Ids.toSet()).size)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET pagination with pack_id filter returns correct total and pages`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId1 = UUID.randomUUID()
        val packId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId1, eventId)
                insertMockedSponsoringPack(packId2, eventId)

                // 3 partnerships for pack1
                repeat(3) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId1,
                    )
                }
                // 2 partnerships for pack2
                repeat(2) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId2,
                    )
                }
            }
        }

        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[pack_id]=$packId1&page=1&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())
        assertEquals(1, body.page)
        assertEquals(2, body.pageSize)
        assertEquals(3, body.total)
        assertEquals(2, body.items.size)

        val responsePage2 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships?filter[pack_id]=$packId1&page=2&page_size=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage2.status)
        val page2 = json.decodeFromString<PaginatedResponse<PartnershipItem>>(responsePage2.bodyAsText())
        assertEquals(2, page2.page)
        assertEquals(3, page2.total)
        assertEquals(1, page2.items.size)
    }
}
