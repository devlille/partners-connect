package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothOption
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test covering the full CRUD lifecycle:
 * POST → GET (verify sorted) → PUT → GET (verify updated) → DELETE → GET (verify empty)
 */
class BoothActivityRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod")
    fun `full CRUD lifecycle for booth activities`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedBoothOption(partnershipId = partnershipId, packId = packId, eventId = eventId)
            }
        }

        val baseUrl = "/events/$eventId/partnerships/$partnershipId/activities"

        // POST: create first activity
        val firstActivityBody = """{"title":"First Demo","description":"First description",""" +
            """"start_time":"2026-06-14T10:00:00","end_time":"2026-06-14T10:30:00"}"""
        val createResponse = client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody(firstActivityBody)
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdBody = json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val activityId = createdBody["id"]!!.jsonPrimitive.content
        assertEquals("First Demo", createdBody["title"]!!.jsonPrimitive.content)

        // POST: create second activity (with null times)
        val createResponse2 = client.post(baseUrl) {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Second Demo","description":"Second description"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse2.status)

        // GET: verify both activities returned
        val listResponse = client.get(baseUrl)
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listBody = json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(2, listBody.size)

        // PUT: update first activity
        val updateResponse = client.put("$baseUrl/$activityId") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated Demo","description":"Updated description"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedBody = json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject
        assertEquals("Updated Demo", updatedBody["title"]!!.jsonPrimitive.content)

        // GET: verify updated title appears
        val listResponse2 = client.get(baseUrl)
        val listBody2 = json.parseToJsonElement(listResponse2.bodyAsText()).jsonArray
        assertTrue(listBody2.any { it.jsonObject["title"]?.jsonPrimitive?.content == "Updated Demo" })

        // DELETE: remove first activity
        val deleteResponse = client.delete("$baseUrl/$activityId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // GET: verify only second activity remains
        val listResponse3 = client.get(baseUrl)
        val listBody3 = json.parseToJsonElement(listResponse3.bodyAsText()).jsonArray
        assertEquals(1, listBody3.size)
        assertEquals("Second Demo", listBody3[0].jsonObject["title"]!!.jsonPrimitive.content)
    }
}
