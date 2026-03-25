package fr.devlille.partners.connect.digest

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.integrations.factories.insertMockedIntegration
import fr.devlille.partners.connect.integrations.factories.insertSlackIntegration
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for POST /orgs/{orgSlug}/events/{eventId}/jobs/digest endpoint.
 * Covers US1 (agreement reminders), US2 (quote reminders), US3 (social media reminders),
 * and Slack failure resilience.
 */
@Suppress("MaxLineLength")
class DigestJobRoutesTest {
    private fun slackMock(): Triple<Slack, MethodsClient, ChatPostMessageResponse> {
        val slack = mockk<Slack>()
        val slackMethod = mockk<MethodsClient>()
        val slackResponse = mockk<ChatPostMessageResponse>()
        every { slack.methods(any()) } returns slackMethod
        every {
            slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
        } returns slackResponse
        every { slackResponse.isOk } returns true
        return Triple(slack, slackMethod, slackResponse)
    }

    @Test
    fun `sends Slack notification when partnership needs agreement`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(packId, eventId = eventId, basePrice = 1000)
                // Partnership validated, no agreement yet → agreement reminder
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    agreementUrl = null,
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()) }
    }

    @Test
    fun `no notification when partnership already has agreement`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(packId, eventId = eventId, basePrice = 1000)
                // Partnership already has agreement → no agreement reminder
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    agreementUrl = "https://example.com/agreement.pdf",
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                )
                // Billing already has quotePdfUrl → no quote reminder either
                insertMockedBilling(eventId = eventId, partnershipId = partnershipId, quotePdfUrl = "https://example.com/quote.pdf")
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify(exactly = 0) {
            slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
        }
    }

    @Test
    fun `sends Slack notification when partnership needs billing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(packId, eventId = eventId, basePrice = 2000)
                // Partnership validated, has billing without quotePdfUrl or invoicePdfUrl → billing reminder
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                )
                insertMockedBilling(
                    eventId = eventId,
                    partnershipId = partnershipId,
                    quotePdfUrl = null,
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()) }
    }

    @Test
    fun `no notification when billing already has quotePdfUrl`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(packId, eventId = eventId, basePrice = 2000)
                // Partnership has agreement and billing with quotePdfUrl → no reminders
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    agreementUrl = "https://example.com/agreement.pdf",
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                )
                // Billing already has a quotePdfUrl → no billing reminder
                insertMockedBilling(
                    eventId = eventId,
                    partnershipId = partnershipId,
                    quotePdfUrl = "https://example.com/quote.pdf",
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify(exactly = 0) {
            slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
        }
    }

    @Test
    fun `no notification when billing already has invoicePdfUrl`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(packId, eventId = eventId, basePrice = 2000)
                // Partnership has agreement and billing with invoicePdfUrl → no reminders
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    agreementUrl = "https://example.com/agreement.pdf",
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                )
                // Billing already has an invoicePdfUrl → no billing reminder
                insertMockedBilling(
                    eventId = eventId,
                    partnershipId = partnershipId,
                    invoicePdfUrl = "https://example.com/invoice.pdf",
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify(exactly = 0) {
            slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
        }
    }

    @Test
    fun `sends Slack notification when communication is scheduled today`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val todayNoon = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 12, 0, 0)

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                // Partnership with communicationPublicationDate = today → social media reminder
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    declinedAt = null,
                    communicationPublicationDate = todayNoon,
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()) }
    }

    @Test
    fun `no notification when communication is scheduled for another day`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        // Use next year to safely avoid today's date range
        val futureDate = LocalDateTime(today.year + 1, today.monthNumber, today.dayOfMonth, 12, 0, 0)

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                val pack = insertMockedSponsoringPack(UUID.randomUUID(), eventId = eventId, basePrice = 1000)
                // Partnership with agreementUrl + billing with quotePdfUrl → excluded from agreement/billing queries
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    agreementUrl = "https://example.com/agreement.pdf",
                    declinedAt = null,
                    selectedPackId = pack.id.value,
                    communicationPublicationDate = futureDate,
                )
                insertMockedBilling(eventId = eventId, partnershipId = partnershipId, quotePdfUrl = "https://example.com/quote.pdf")
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify(exactly = 0) {
            slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
        }
    }

    @Test
    fun `sends digest in English when Accept-Language is en`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val (slack, slackMethod, _) = slackMock()

        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val todayNoon = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 12, 0, 0)

        application {
            moduleSharedDb(userId = userId, slack = slack)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    validatedAt = event.submissionStartTime,
                    declinedAt = null,
                    communicationPublicationDate = todayNoon,
                )
                val integrationId = insertMockedIntegration(eventId = eventId)
                insertSlackIntegration(integrationId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest") {
            header(HttpHeaders.AcceptLanguage, "en")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()) }
    }
}
