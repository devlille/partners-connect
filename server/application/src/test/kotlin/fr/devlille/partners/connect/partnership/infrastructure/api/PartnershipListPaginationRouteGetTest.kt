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
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipListPaginationRouteGetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns default pagination when no page params provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

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
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.page)
        assertEquals(20, body.pageSize)
        assertEquals(1, body.total)
        assertEquals(1, body.items.size)
    }

    @Test
    @Suppress("LongMethod")
    fun `GET with page and page_size returns correct page`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId)
                repeat(5) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                    )
                }
            }
        }

        val responsePage1 = client.get("/orgs/$orgId/events/$eventId/partnerships?page=1&page_size=2") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage1.status)
        val page1 = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            responsePage1.bodyAsText(),
        )
        assertEquals(1, page1.page)
        assertEquals(2, page1.pageSize)
        assertEquals(5, page1.total)
        assertEquals(2, page1.items.size)

        val responsePage2 = client.get("/orgs/$orgId/events/$eventId/partnerships?page=2&page_size=2") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage2.status)
        val page2 = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            responsePage2.bodyAsText(),
        )
        assertEquals(2, page2.page)
        assertEquals(2, page2.pageSize)
        assertEquals(5, page2.total)
        assertEquals(2, page2.items.size)

        val responsePage3 = client.get("/orgs/$orgId/events/$eventId/partnerships?page=3&page_size=2") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responsePage3.status)
        val page3 = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            responsePage3.bodyAsText(),
        )
        assertEquals(3, page3.page)
        assertEquals(2, page3.pageSize)
        assertEquals(5, page3.total)
        assertEquals(1, page3.items.size)
    }

    @Test
    fun `GET page beyond total returns empty items`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

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
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?page=10&page_size=5") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(10, body.page)
        assertEquals(5, body.pageSize)
        assertEquals(1, body.total)
        assertEquals(0, body.items.size)
    }

    @Test
    fun `GET pages contain different items - no duplicates`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId)
                repeat(4) {
                    val companyId = UUID.randomUUID()
                    insertMockedCompany(companyId)
                    insertMockedPartnership(
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                    )
                }
            }
        }

        val responsePage1 = client.get("/orgs/$orgId/events/$eventId/partnerships?page=1&page_size=2") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val responsePage2 = client.get("/orgs/$orgId/events/$eventId/partnerships?page=2&page_size=2") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        val page1 = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            responsePage1.bodyAsText(),
        )
        val page2 = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            responsePage2.bodyAsText(),
        )

        val page1Ids = page1.items.map { it.id }
        val page2Ids = page2.items.map { it.id }
        assertEquals(0, page1Ids.intersect(page2Ids.toSet()).size)
    }

    @Test
    fun `GET with invalid page params falls back to defaults`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships?page=abc&page_size=xyz") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<PaginatedResponse<PartnershipItem, PartnershipListMetadata>>(
            response.bodyAsText(),
        )
        assertEquals(1, body.page)
        assertEquals(20, body.pageSize)
    }
}
