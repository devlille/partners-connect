package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedSpeakerPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for speaker-partnership attachment/detachment endpoints.
 * Tests JSON schema validation for speaker_partnership.schema.json.
 * Validates that endpoints correctly handle valid and invalid requests.
 */
class SpeakerPartnershipRoutesContractTest {
    @Test
    fun `POST validates schema successfully`() = testApplication {
        val eventSlug = "test-event-schema"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()

            // Create test data
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(eventId, eventSlug, organisation = org)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedSpeaker(
                id = speakerId,
                eventId = eventId,
            )
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // Should return 201 Created with valid speaker partnership response
        assertEquals(HttpStatusCode.Created, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["partnership_id"]?.jsonPrimitive?.content)
        assertEquals(speakerId.toString(), responseBody["speaker_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `POST returns 409 for duplicate attachment`() = testApplication {
        val eventSlug = "test-event-dup"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()

            // Create test data
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(eventId, eventSlug, organisation = org)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedSpeaker(
                id = speakerId,
                eventId = eventId,
            )
        }

        // First attachment should succeed
        val firstResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Second attachment should fail with conflict
        val secondResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }

    @Test
    fun `DELETE returns 204 on successful detachment`() = testApplication {
        val eventSlug = "test-event-delete"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()

            // Create test data
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(eventId, eventSlug, organisation = org)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedSpeaker(
                id = speakerId,
                eventId = eventId,
            )

            // Create the attachment first
            insertMockedSpeakerPartnership(partnershipId = partnershipId, speakerId = speakerId)
        }

        val response = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE returns 404 for non-existent association`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 403 for non-validated partnership`() = testApplication {
        val eventSlug = "test-event-forbidden"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()

            // Create test data but do NOT validate the partnership
            val org = insertMockedOrganisationEntity(orgId)
            insertMockedEventWithOrga(eventId, eventSlug, organisation = org)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = null,
            )
            insertMockedSpeaker(
                id = speakerId,
                eventId = eventId,
            )
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // Should fail if partnership is not validated
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST returns 404 for non-existent partnership`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 for non-existent speaker`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
