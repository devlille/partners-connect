package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedSupportVideo
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipSupportVideoGetRouteTest {
    @Test
    fun `GET returns 200 with current support video submission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val videoId = UUID.randomUUID()
        val slug = "dev-fest-get-200-${UUID.randomUUID()}"
        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = slug)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedCompany(companyId)
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

        val response = client.get("/events/$slug/partnerships/$partnershipId/support-video")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(videoId.toString(), body["id"]?.jsonPrimitive?.content)
        assertEquals(partnershipId.toString(), body["partnership_id"]?.jsonPrimitive?.content)
        assertEquals(slug, body["event_slug"]?.jsonPrimitive?.content)
        assertEquals("pending", body["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET returns 404 when no video has been submitted`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val slug = "dev-fest-get-no-video-${UUID.randomUUID()}"
        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = slug)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.get("/events/$slug/partnerships/$partnershipId/support-video")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 404 when partnership does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "dev-fest-get-no-partnership-${UUID.randomUUID()}"
        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = slug)
            }
        }

        val response = client.get("/events/$slug/partnerships/${UUID.randomUUID()}/support-video")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
