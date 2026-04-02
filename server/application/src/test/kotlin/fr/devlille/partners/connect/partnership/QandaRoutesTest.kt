package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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

class QandaRoutesTest {
    @Suppress("LongMethod")
    @Test
    fun `full Q&A CRUD lifecycle - create, list, update, delete`() = testApplication {
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
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 5
                event.qandaMaxAnswers = 4
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

        // Step 1: Create a question
        val createBody = """
            {
                "question": "What year was Acme Corp founded?",
                "answers": [
                    {"answer": "2010", "is_correct": false},
                    {"answer": "2015", "is_correct": true},
                    {"answer": "2020", "is_correct": false}
                ]
            }
        """.trimIndent()
        val createResponse =
            client.post("/events/$eventId/partnerships/$partnershipId/qanda/questions") {
                contentType(ContentType.Application.Json)
                setBody(createBody)
            }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val created = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val questionId = created["id"]!!.jsonPrimitive.content
        assertTrue(created["question"]!!.jsonPrimitive.content.contains("Acme Corp"))
        assertEquals(3, created["answers"]!!.jsonArray.size)

        // Step 2: List questions for partnership
        val listResponse =
            client.get("/events/$eventId/partnerships/$partnershipId/qanda/questions")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listBody = Json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertEquals(1, listBody.size)

        // Step 3: Update the question
        val updateBody = """
            {
                "question": "In which year was Acme Corp founded?",
                "answers": [
                    {"answer": "2010", "is_correct": false},
                    {"answer": "2015", "is_correct": true},
                    {"answer": "2018", "is_correct": false},
                    {"answer": "2020", "is_correct": false}
                ]
            }
        """.trimIndent()
        val updateResponse =
            client.put("/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId") {
                contentType(ContentType.Application.Json)
                setBody(updateBody)
            }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val updated = Json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject
        assertTrue(updated["question"]!!.jsonPrimitive.content.contains("In which year"))
        assertEquals(4, updated["answers"]!!.jsonArray.size)

        // Step 4: Delete the question
        val deleteResponse =
            client.delete("/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Step 5: Verify empty list
        val emptyListResponse =
            client.get("/events/$eventId/partnerships/$partnershipId/qanda/questions")
        assertEquals(HttpStatusCode.OK, emptyListResponse.status)
        val emptyList = Json.parseToJsonElement(emptyListResponse.bodyAsText()).jsonArray
        assertEquals(0, emptyList.size)
    }

    @Suppress("LongMethod")
    @Test
    fun `event-level Q&A listing with multiple partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 5
                event.qandaMaxAnswers = 4
                insertMockedCompany(companyId1, name = "Acme Corp")
                insertMockedCompany(companyId2, name = "Beta Inc")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId1,
                    eventId = eventId,
                    companyId = companyId1,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = partnershipId2,
                    eventId = eventId,
                    companyId = companyId2,
                    selectedPackId = packId,
                )
            }
        }

        // Create question for partnership 1
        val body1 = """
            {
                "question": "Acme question?",
                "answers": [
                    {"answer": "Yes", "is_correct": true},
                    {"answer": "No", "is_correct": false}
                ]
            }
        """.trimIndent()
        client.post("/events/$eventId/partnerships/$partnershipId1/qanda/questions") {
            contentType(ContentType.Application.Json)
            setBody(body1)
        }

        // Create question for partnership 2
        val body2 = """
            {
                "question": "Beta question?",
                "answers": [
                    {"answer": "A", "is_correct": false},
                    {"answer": "B", "is_correct": true}
                ]
            }
        """.trimIndent()
        client.post("/events/$eventId/partnerships/$partnershipId2/qanda/questions") {
            contentType(ContentType.Application.Json)
            setBody(body2)
        }

        // Get all event questions
        val response = client.get("/events/$eventId/qanda/questions")
        assertEquals(HttpStatusCode.OK, response.status)

        val eventQuestions = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, eventQuestions.size)

        val companyNames = eventQuestions.map {
            it.jsonObject["company_name"]!!.jsonPrimitive.content
        }.toSet()
        assertTrue(companyNames.contains("Acme Corp"))
        assertTrue(companyNames.contains("Beta Inc"))
    }
}
