package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaAnswer
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaQuestion
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QandaEventQuestionsRouteGetTest {
    @Test
    fun `GET returns questions grouped by partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()
        val questionId1 = UUID.randomUUID()
        val questionId2 = UUID.randomUUID()

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
                insertMockedQandaQuestion(id = questionId1, partnershipId = partnershipId1, question = "Q1")
                insertMockedQandaAnswer(questionId = questionId1, answer = "A1", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId1, answer = "A2", isCorrect = false)
                insertMockedQandaQuestion(id = questionId2, partnershipId = partnershipId2, question = "Q2")
                insertMockedQandaAnswer(questionId = questionId2, answer = "B1", isCorrect = false)
                insertMockedQandaAnswer(questionId = questionId2, answer = "B2", isCorrect = true)
            }
        }

        val response = client.get("/events/$eventId/qanda/questions")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Acme Corp"))
        assertTrue(body.contains("Beta Inc"))
        assertTrue(body.contains("Q1"))
        assertTrue(body.contains("Q2"))
    }

    @Test
    fun `GET returns 403 when Q&A disabled`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/events/$eventId/qanda/questions")

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET returns empty list when no questions exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
            }
        }

        val response = client.get("/events/$eventId/qanda/questions")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }
}
