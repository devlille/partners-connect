package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration test for complete speaker-partnership workflow scenarios.
 * Tests end-to-end business logic from agenda import to speaker attachment.
 * Follows constitutional requirements for integration testing via HTTP routes.
 */
@Suppress("LongMethod")
class SpeakerPartnershipWorkflowIntegrationTest {
    @Test
    fun `complete import and attachment workflow works end-to-end`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org-slug"
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = eventId,
                companyId = companyId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
        }

        // Step 1: Attach speaker to partnership
        val attachResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Created, attachResponse.status)

        val attachmentJson = Json.parseToJsonElement(attachResponse.bodyAsText()).jsonObject
        assertEquals(speakerId.toString(), attachmentJson["speaker_id"]?.jsonPrimitive?.content)
        assertEquals(partnershipId.toString(), attachmentJson["partnership_id"]?.jsonPrimitive?.content)
        assertNotNull(attachmentJson["created_at"])

        // Step 2: Verify partnership details include speaker
        val partnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, partnershipResponse.status)

        val partnershipJson = Json.parseToJsonElement(partnershipResponse.bodyAsText()).jsonObject
        val partnershipSpeakers = partnershipJson["speakers"]?.jsonArray!!
        assertEquals(1, partnershipSpeakers.size, "Partnership should have 1 attached speaker")
        assertEquals(speakerId.toString(), partnershipSpeakers[0].jsonObject["id"]?.jsonPrimitive?.content)

        // Step 3: Test speaker detachment
        val detachResponse = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.NoContent, detachResponse.status)

        // Step 4: Verify speaker is removed from partnership
        val finalPartnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, finalPartnershipResponse.status)

        val finalPartnershipJson = Json.parseToJsonElement(finalPartnershipResponse.bodyAsText()).jsonObject
        val finalSpeakers = finalPartnershipJson["speakers"]?.jsonArray!!
        assertEquals(0, finalSpeakers.size, "Speaker should be removed from partnership")
    }

    @Test
    fun `error handling for invalid speaker attachment scenarios`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val eventSlug = "test-event"
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@test.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val nonExistentPartnershipId = UUID.randomUUID()
        val nonExistentSpeakerId = UUID.randomUUID()

        // Test 1: Attach to non-existent partnership
        val response1 =
            client.post("/events/$eventSlug/partnerships/$nonExistentPartnershipId/speakers/$nonExistentSpeakerId") {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
        assertEquals(HttpStatusCode.NotFound, response1.status)

        // Test 2: Detach from non-existent association
        val response2 =
            client.delete("/events/$eventSlug/partnerships/$nonExistentPartnershipId/speakers/$nonExistentSpeakerId")
        assertEquals(HttpStatusCode.NotFound, response2.status)
    }

    @Test
    fun `duplicate speaker attachment returns conflict error`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org-slug"
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                partnershipId,
                eventId = eventId,
                companyId = companyId,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speakerId, eventId = eventId)
        }

        // First attachment should succeed
        val firstResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Second attachment should fail with conflict
        val secondResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }
}
