package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaAnswer
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaQuestion
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.webhooks.application.mappers.toWebhookQuestion
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class QandaWebhookMapperTest {
    @Test
    fun `maps question fields and indexes answers, preserving is_correct`() = testApplication {
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
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "Acme Corp")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedQandaQuestion(id = questionId, partnershipId = partnershipId, question = "What is Kotlin?")
                insertMockedQandaAnswer(questionId = questionId, answer = "A language", isCorrect = true)
                insertMockedQandaAnswer(questionId = questionId, answer = "A drink", isCorrect = false)
                insertMockedQandaAnswer(questionId = questionId, answer = "An island", isCorrect = false)
            }
        }
        client.get("/")

        transaction {
            val result = QandaQuestionEntity[questionId].toWebhookQuestion(order = 5)

            assertEquals(questionId.toString(), result.id)
            assertEquals("What is Kotlin?", result.question)
            assertEquals(5, result.order)
            assertEquals(3, result.answers.size)
            // order is derived from collection position, always 0,1,2 for three answers
            assertEquals(listOf(0, 1, 2), result.answers.map { it.order })
            assertEquals(1, result.answers.count { it.isCorrect })
            assertEquals("A language", result.answers.single { it.isCorrect }.answer)
            assertEquals(setOf("A language", "A drink", "An island"), result.answers.map { it.answer }.toSet())
        }
    }
}
