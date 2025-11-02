package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
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
        val eventSlug = "test-post-approves-a-sugg-213"
        val companyId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnerId, eventId = eventId, companyId = companyId)
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnerId/suggestion-approve")
        assertEquals(HttpStatusCode.OK, response.status)

        transaction {
            val entity = PartnershipEntity[partnerId]
            assertTrue(entity.suggestionApprovedAt != null)
        }
    }

    @Test
    fun `POST declines a suggestion`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-declines-a-sugg-979"
        val companyId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnerId, eventId = eventId, companyId = companyId)
        }

        val response = client.post("/events/$eventSlug/partnerships/$partnerId/suggestion-decline")
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
            "/events/$randomUUID/partnerships/$randomUUID/suggestion-approve",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST decline returns 404 for unknown partnership`() = testApplication {
        val randomUUID = UUID.randomUUID()
        val response = client.post(
            "/events/$randomUUID/partnerships/$randomUUID/suggestion-decline",
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
