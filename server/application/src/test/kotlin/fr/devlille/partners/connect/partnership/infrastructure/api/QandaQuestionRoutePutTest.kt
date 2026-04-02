package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaAnswer
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaQuestion
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QandaQuestionRoutePutTest {
    @Test
    fun `PUT updates question and returns 200`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val questionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId)
                insertMockedQandaAnswer(questionId = questionId, answer = "Old A", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "Old B", isCorrect = false)
            }
        }

        val body = """
            {
                "question": "Updated question?",
                "answers": [
                    {"answer": "New A", "is_correct": false},
                    {"answer": "New B", "is_correct": true}
                ]
            }
        """.trimIndent()
        val response =
            client.put("/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Updated question?"))
        assertTrue(responseBody.contains("New A"))
        assertTrue(responseBody.contains("New B"))
    }

    @Test
    fun `PUT returns 404 when question not found`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val fakeQuestionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
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

        val body = """
            {
                "question": "Test?",
                "answers": [
                    {"answer": "A", "is_correct": true},
                    {"answer": "B", "is_correct": false}
                ]
            }
        """.trimIndent()
        val response =
            client.put("/events/$eventId/partnerships/$partnershipId/qanda/questions/$fakeQuestionId") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 400 when validation fails`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val questionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId)
                insertMockedQandaAnswer(questionId = questionId, answer = "A", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "B", isCorrect = false)
            }
        }

        val body = """
            {
                "question": "Test?",
                "answers": [
                    {"answer": "A", "is_correct": false},
                    {"answer": "B", "is_correct": false}
                ]
            }
        """.trimIndent()
        val response =
            client.put("/events/$eventId/partnerships/$partnershipId/qanda/questions/$questionId") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
