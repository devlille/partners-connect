package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipEmailHistory
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract test for GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/email-history endpoint.
 * Tests HTTP contract (request/response schemas) for viewing partnership email history.
 */
class PartnershipEmailHistoryRouteGetTest {
    @Test
    fun `GET returns paginated email history for existing partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val emailHistoryId = UUID.randomUUID()

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
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedPartnershipEmailHistory(
                    id = emailHistoryId,
                    partnershipId = partnershipId,
                    subject = "Partnership Approved",
                    senderEmail = "no-reply@devlille.fr",
                    overallStatus = OverallDeliveryStatus.SENT,
                    recipientEmails = listOf("partner@company.com", "contact@company.com"),
                    recipientStatuses = mapOf(
                        "partner@company.com" to DeliveryStatus.SENT,
                        "contact@company.com" to DeliveryStatus.SENT,
                    ),
                    triggeredBy = userId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(emailHistoryId.toString()))
        assertTrue(body.contains("Partnership Approved"))
        assertTrue(body.contains("partner@company.com"))
    }

    @Test
    fun `GET returns empty list for partnership with no email history`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"items\":[]") || body.contains("\"items\": []"))
    }

    @Test
    fun `GET supports pagination with page and pageSize parameters`() = testApplication {
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

                // Create multiple email history records
                repeat(5) { index ->
                    insertMockedPartnershipEmailHistory(
                        partnershipId = partnershipId,
                        subject = "Email $index",
                        triggeredBy = userId,
                    )
                }
            }
        }

        val response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history?page=1&pageSize=2",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"total\":5") || body.contains("\"total\": 5"))
    }

    @Test
    fun `GET returns 401 when unauthorized`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                // No permission granted
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history") {
            header(HttpHeaders.Authorization, "Bearer invalid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
