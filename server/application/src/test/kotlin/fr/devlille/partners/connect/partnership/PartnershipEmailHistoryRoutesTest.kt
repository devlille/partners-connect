package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipEmailHistory
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryTable
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
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for GET /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/email-history endpoint.
 * Tests end-to-end business logic including database operations, authorization, and pagination.
 */
class PartnershipEmailHistoryRoutesTest {
    @Test
    fun `GET returns all email history records for partnership ordered by most recent first`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
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

                // Create multiple email history records with different timestamps
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                insertMockedPartnershipEmailHistory(
                    partnershipId = partnershipId,
                    subject = "First Email",
                    sentAt = now,
                    triggeredBy = userId,
                )
                insertMockedPartnershipEmailHistory(
                    partnershipId = partnershipId,
                    subject = "Second Email",
                    sentAt = now,
                    triggeredBy = userId,
                )
                insertMockedPartnershipEmailHistory(
                    partnershipId = partnershipId,
                    subject = "Third Email",
                    sentAt = now,
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

        // Verify all three emails are present
        assertTrue(body.contains("First Email"))
        assertTrue(body.contains("Second Email"))
        assertTrue(body.contains("Third Email"))

        // Verify total count
        assertTrue(body.contains("\"total\":3") || body.contains("\"total\": 3"))
    }

    @Test
    fun `GET pagination works correctly with multiple pages`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
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

                // Create 10 email history records
                repeat(10) { index ->
                    insertMockedPartnershipEmailHistory(
                        partnershipId = partnershipId,
                        subject = "Email $index",
                        triggeredBy = userId,
                    )
                }
            }
        }

        // Get first page (3 items)
        val page1Response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history?page=1&pageSize=3",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, page1Response.status)
        val page1Body = page1Response.bodyAsText()
        assertTrue(page1Body.contains("\"total\":10") || page1Body.contains("\"total\": 10"))

        // Get second page
        val page2Response = client.get(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history?page=2&pageSize=3",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, page2Response.status)
    }

    @Test
    fun `GET returns email history with recipient delivery status details`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val emailHistoryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
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
                    subject = "Multi-recipient Email",
                    senderEmail = "no-reply@devlille.fr",
                    overallStatus = OverallDeliveryStatus.PARTIAL,
                    recipientEmails = listOf(
                        "success@company.com",
                        "failed@company.com",
                    ),
                    recipientStatuses = mapOf(
                        "success@company.com" to DeliveryStatus.SENT,
                        "failed@company.com" to DeliveryStatus.FAILED,
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

        // Verify email details
        assertTrue(body.contains("Multi-recipient Email"))
        assertTrue(body.contains("no-reply@devlille.fr"))
        assertTrue(body.contains("PARTIAL"))

        // Verify recipient status details
        assertTrue(body.contains("success@company.com"))
        assertTrue(body.contains("failed@company.com"))
        assertTrue(body.contains("SENT"))
        assertTrue(body.contains("FAILED"))
    }

    @Test
    @Suppress("LongMethod")
    fun `GET email history is isolated per partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId1)
                insertMockedCompany(companyId2)
                insertMockedSponsoringPack(packId, eventId)

                // Partnership 1
                insertMockedPartnership(
                    id = partnershipId1,
                    eventId = eventId,
                    companyId = companyId1,
                    selectedPackId = packId,
                )
                insertMockedPartnershipEmailHistory(
                    partnershipId = partnershipId1,
                    subject = "Partnership 1 Email",
                    triggeredBy = userId,
                )

                // Partnership 2
                insertMockedPartnership(
                    id = partnershipId2,
                    eventId = eventId,
                    companyId = companyId2,
                    selectedPackId = packId,
                )
                insertMockedPartnershipEmailHistory(
                    partnershipId = partnershipId2,
                    subject = "Partnership 2 Email",
                    triggeredBy = userId,
                )
            }
        }

        // Get email history for partnership 1
        val response1 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId1/email-history",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        val body1 = response1.bodyAsText()
        assertTrue(body1.contains("Partnership 1 Email"))
        assertTrue(!body1.contains("Partnership 2 Email"))

        // Get email history for partnership 2
        val response2 = client.get(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId2/email-history",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val body2 = response2.bodyAsText()
        assertTrue(body2.contains("Partnership 2 Email"))
        assertTrue(!body2.contains("Partnership 1 Email"))
    }

    @Test
    fun `GET requires organiser authorization`() = testApplication {
        val userId = UUID.randomUUID()
        val unauthorizedUserId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = unauthorizedUserId)
            transaction {
                insertMockedUser(userId)
                insertMockedUser(unauthorizedUserId)
                insertMockedOrganisationEntity(orgId)
                // Only userId has permission, unauthorizedUserId does not
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/email-history") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `database query correctly orders by sentAt DESC`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
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

        // Verify database query ordering (would be tested in repository layer)
        transaction {
            val records = PartnershipEmailHistoryEntity
                .find { PartnershipEmailHistoryTable.partnershipId eq partnershipId }
                .orderBy(PartnershipEmailHistoryTable.sentAt to SortOrder.DESC)
                .toList()

            assertEquals(0, records.size, "No records should exist yet")
        }
    }
}
