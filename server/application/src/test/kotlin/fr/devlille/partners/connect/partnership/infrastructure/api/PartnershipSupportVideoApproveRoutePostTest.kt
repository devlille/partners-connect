package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedSupportVideo
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipSupportVideoApproveRoutePostTest {
    @Test
    fun `organizer can approve pending support video`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()

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
                insertMockedSupportVideo(
                    id = videoId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.PENDING,
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$videoId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("APPROVED", body["status"]?.jsonPrimitive?.content)
        assertEquals(videoId.toString(), body["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `approve returns 401 when no JWT provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()

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
                insertMockedSupportVideo(
                    id = videoId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.PENDING,
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$videoId/approve",
        ) {
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `approve returns 401 when user lacks org permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                // No insertMockedOrgaPermission -> user has no permission on the org
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedSupportVideo(
                    id = videoId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.PENDING,
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$videoId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `approve returns 404 when support video does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val missingVideoId = UUID.randomUUID()

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

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$missingVideoId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `approve returns 409 when support video is already approved`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()

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
                insertMockedSupportVideo(
                    id = videoId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.APPROVED,
                    reviewerUserId = userId,
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$videoId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `approve returns 409 when support video is already declined`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()

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
                insertMockedSupportVideo(
                    id = videoId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.DECLINED,
                    reviewerUserId = userId,
                    declineReason = "Not relevant",
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/support-videos/$videoId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
