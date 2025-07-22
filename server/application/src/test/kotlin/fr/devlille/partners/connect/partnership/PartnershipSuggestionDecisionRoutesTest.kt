package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
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
            transaction {
                CompanyEntity.new(companyId) {
                    name = "Test Company"
                    siteUrl = "https://example.com"
                }
                PartnershipEntity.new(partnerId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnerId/approve")
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
            transaction {
                CompanyEntity.new(companyId) {
                    name = "Test Company"
                    siteUrl = "https://example.com"
                }
                PartnershipEntity.new(partnerId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnerId/decline")
        println(response.bodyAsText())
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
        val response = client.post(
            "/events/${UUID.randomUUID()}/companies/${UUID.randomUUID()}/partnership/${UUID.randomUUID()}/approve",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST decline returns 404 for unknown partnership`() = testApplication {
        val response = client.post(
            "/events/${UUID.randomUUID()}/companies/${UUID.randomUUID()}/partnership/${UUID.randomUUID()}/decline",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
