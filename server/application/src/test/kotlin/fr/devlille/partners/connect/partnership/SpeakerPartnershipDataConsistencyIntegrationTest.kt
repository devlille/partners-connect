package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
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
import kotlin.test.assertTrue

/**
 * Integration test for speaker-partnership data consistency validation.
 * Tests data integrity constraints and cross-domain validation.
 * Follows constitutional requirements for testing business rules via HTTP routes.
 */
@Suppress("LongMethod")
class SpeakerPartnershipDataConsistencyIntegrationTest {
    @Test
    fun `agenda correctly aggregates speakers from multiple partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org-slug"
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()
        val speaker1Id = UUID.randomUUID()
        val speaker2Id = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(company1Id)
            insertMockedCompany(company2Id)
            insertMockedPartnership(
                partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedPartnership(
                partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            )
            insertMockedSpeaker(speaker1Id, eventId = eventId)
            insertMockedSpeaker(speaker2Id, eventId = eventId)
        }

        // Step 1: Get speakers from agenda (public endpoint)
        val agendaResponse = client.get("/events/$eventSlug/agenda")
        assertEquals(HttpStatusCode.OK, agendaResponse.status)

        val agendaJson = Json.parseToJsonElement(agendaResponse.bodyAsText()).jsonObject
        val speakers = agendaJson["speakers"]?.jsonArray!!
        assertTrue(speakers.size >= 2, "Need at least 2 speakers for this test")
        assertEquals(speaker1Id.toString(), speakers[0].jsonObject["id"]?.jsonPrimitive?.content!!)
        assertEquals(speaker2Id.toString(), speakers[1].jsonObject["id"]?.jsonPrimitive?.content!!)

        // Step 2: Create multiple partnerships and attach different speakers
        val attach1Response = client.post("/events/$eventSlug/partnerships/$partnership1Id/speakers/$speaker1Id") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, attach1Response.status)

        // Attach speaker2 to partnership2
        val attach2Response = client.post("/events/$eventSlug/partnerships/$partnership2Id/speakers/$speaker2Id") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, attach2Response.status)

        // Step 3: Verify each partnership shows its correct speaker
        val partnership1Response = client.get("/events/$eventSlug/partnerships/$partnership1Id")
        assertEquals(HttpStatusCode.OK, partnership1Response.status)

        val partnership1Json = Json.parseToJsonElement(partnership1Response.bodyAsText()).jsonObject
        val partnership1Speakers = partnership1Json["speakers"]?.jsonArray!!
        assertEquals(1, partnership1Speakers.size, "Partnership1 should have 1 speaker")
        assertEquals(speaker1Id.toString(), partnership1Speakers[0].jsonObject["id"]?.jsonPrimitive?.content)

        val partnership2Response = client.get("/events/$eventSlug/partnerships/$partnership2Id")
        assertEquals(HttpStatusCode.OK, partnership2Response.status)

        val partnership2Json = Json.parseToJsonElement(partnership2Response.bodyAsText()).jsonObject
        val partnership2Speakers = partnership2Json["speakers"]?.jsonArray!!
        assertEquals(1, partnership2Speakers.size, "Partnership2 should have 1 speaker")
        assertEquals(speaker2Id.toString(), partnership2Speakers[0].jsonObject["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `speaker attachment maintains data consistency across database operations`() = testApplication {
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
        }
        assertEquals(HttpStatusCode.Created, attachResponse.status)

        // Step 2: Verify attachment is reflected in partnership details
        val beforePartnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, beforePartnershipResponse.status)

        val beforeJson = Json.parseToJsonElement(beforePartnershipResponse.bodyAsText()).jsonObject
        val beforeSpeakers = beforeJson["speakers"]?.jsonArray!!
        assertEquals(1, beforeSpeakers.size, "Partnership should have 1 speaker before detachment")

        // Step 3: Detach speaker
        val detachResponse = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")
        assertEquals(HttpStatusCode.NoContent, detachResponse.status)

        // Step 4: Verify detachment is reflected in partnership details
        val afterPartnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, afterPartnershipResponse.status)

        val afterJson = Json.parseToJsonElement(afterPartnershipResponse.bodyAsText()).jsonObject
        val afterSpeakers = afterJson["speakers"]?.jsonArray!!
        assertEquals(0, afterSpeakers.size, "Speaker should be removed from partnership")
    }

    @Test
    fun `multiple speaker attachments to same partnership work correctly`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org-slug"
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val speaker1Id = UUID.randomUUID()
        val speaker2Id = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val company2Id = UUID.randomUUID()

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
            insertMockedSpeaker(speaker1Id, eventId = eventId)
            insertMockedSpeaker(speaker2Id, eventId = eventId)
        }

        // Step 1: Attach multiple speakers to the same partnership
        val attach1Response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speaker1Id") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, attach1Response.status)

        val attach2Response = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speaker2Id") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Created, attach2Response.status)

        // Step 2: Verify partnership shows both speakers
        val partnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, partnershipResponse.status)

        val partnershipJson = Json.parseToJsonElement(partnershipResponse.bodyAsText()).jsonObject
        val partnershipSpeakers = partnershipJson["speakers"]?.jsonArray!!
        assertEquals(2, partnershipSpeakers.size, "Partnership should have 2 speakers")

        val speakerIds = partnershipSpeakers.map { it.jsonObject["id"]?.jsonPrimitive?.content }.toSet()
        assertTrue(speakerIds.contains(speaker1Id.toString()), "Partnership should contain speaker1")
        assertTrue(speakerIds.contains(speaker2Id.toString()), "Partnership should contain speaker2")
    }

    @Test
    fun `speaker data integrity is maintained after detachment operations`() = testApplication {
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

        // Step 1: Attach and then detach speaker multiple times to test consistency
        repeat(3) {
            // Attach
            val attachResponse = client.post("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId") {
                contentType(ContentType.Application.Json)
            }
            assertEquals(HttpStatusCode.Created, attachResponse.status)

            // Detach
            val detachResponse = client.delete("/events/$eventSlug/partnerships/$partnershipId/speakers/$speakerId")
            assertEquals(HttpStatusCode.NoContent, detachResponse.status)
        }

        // Step 2: Verify speaker still exists in agenda after multiple operations
        val finalAgendaResponse = client.get("/events/$eventSlug/agenda")
        assertEquals(HttpStatusCode.OK, finalAgendaResponse.status)

        val finalAgendaJson = Json.parseToJsonElement(finalAgendaResponse.bodyAsText()).jsonObject
        val finalSpeakers = finalAgendaJson["speakers"]?.jsonArray!!
        val foundSpeaker = finalSpeakers.find { it.jsonObject["id"]?.jsonPrimitive?.content == speakerId.toString() }
        assertTrue(foundSpeaker != null, "Speaker should still exist in agenda after attachment/detachment cycles")

        // Step 3: Verify partnership is clean (no ghost associations)
        val finalPartnershipResponse = client.get("/events/$eventSlug/partnerships/$partnershipId")
        assertEquals(HttpStatusCode.OK, finalPartnershipResponse.status)

        val finalPartnershipJson = Json.parseToJsonElement(finalPartnershipResponse.bodyAsText()).jsonObject
        val finalPartnershipSpeakers = finalPartnershipJson["speakers"]?.jsonArray!!
        assertEquals(0, finalPartnershipSpeakers.size, "Partnership should have no speakers after final detachment")
    }
}
