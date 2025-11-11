package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedSpeakerPartnership
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for speaker-partnership authorization boundaries.
 * Tests authorization requirements and permission validation.
 * Follows constitutional requirements for testing authorization via HTTP routes.
 */
class SpeakerPartnershipAuthorizationIntegrationTest {
    @Test
    fun `speaker attachment works as public endpoint without authorization`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(id = eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = eventId,
                companyId = companyId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
        }

        // Should not require authorization for speaker attachment (public endpoint)
        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `speaker detachment works as public endpoint without authorization`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(id = eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = eventId,
                companyId = companyId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
            insertMockedSpeakerPartnership(
                partnershipId = partnershipId,
                speakerId = speakerId,
            )
        }

        // Should not require authorization for speaker detachment (public endpoint)
        val response = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `partnership must be validated before speaker attachment is allowed`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(id = eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = eventId,
                companyId = companyId,
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
        }

        // Try to attach speaker to non-validated partnership
        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `cross-event speaker attachment is prevented`() = testApplication {
        val eventId = UUID.randomUUID()
        val event2Id = UUID.randomUUID()
        val eventSlug = "test-event"
        val event2Slug = "test-event2"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(id = eventId, slug = eventSlug)
            insertMockedEventWithOrga(id = event2Id, slug = event2Slug)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = event2Id,
                companyId = companyId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
        }

        // Try to attach speaker from different event
        val response = client.post("/events/$event2Slug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
