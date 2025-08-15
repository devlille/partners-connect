package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PartnershipValidationRoutesTest {
    @Test
    fun `POST validates a partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership?.validatedAt)
    }

    @Test
    fun `POST returns 404 if partnership does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/validate") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST declines a partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/decline") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership?.declinedAt)
    }

    @Test
    fun `POST returns 404 if partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.delete("/events/$eventId/partnership/$partnershipId/decline") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
