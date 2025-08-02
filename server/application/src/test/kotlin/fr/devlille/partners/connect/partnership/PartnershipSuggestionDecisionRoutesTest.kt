package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import junit.framework.TestCase.assertTrue
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipSuggestionDecisionRoutesTest {
    @Test
    fun `POST approves a suggestion`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            transaction {
                PartnershipEntity.new(partnerId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnerId/suggestion-approve")
        assertEquals(HttpStatusCode.OK, response.status)

        transaction {
            val entity = PartnershipEntity[partnerId]
            assertTrue(entity.suggestionApprovedAt != null)
        }
    }

    @Test
    fun `POST declines a suggestion`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            transaction {
                PartnershipEntity.new(partnerId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnerId/suggestion-decline")
        assertEquals(HttpStatusCode.OK, response.status)

        transaction {
            val entity = PartnershipEntity[partnerId]
            assertTrue(entity.suggestionDeclinedAt != null)
            assertEquals(null, entity.suggestionApprovedAt)
            assertEquals(null, entity.validatedAt)
        }
    }

    @Test
    fun `POST approve returns 404 for unknown partnership`() = testApplication {
        val randomUUID = UUID.randomUUID()
        val response = client.post(
            "/events/$randomUUID/companies/$randomUUID/partnership/$randomUUID/suggestion-approve",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST decline returns 404 for unknown partnership`() = testApplication {
        val randomUUID = UUID.randomUUID()
        val response = client.post(
            "/events/$randomUUID/companies/$randomUUID/partnership/$randomUUID/suggestion-decline",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
