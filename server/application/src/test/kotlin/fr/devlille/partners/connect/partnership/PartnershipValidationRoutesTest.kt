package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
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
            insertMockCompany(companyId)
            transaction {
                SponsoringPackEntity.new(packId) {
                    this.eventId = eventId
                    name = "Gold"
                    basePrice = 1000
                    maxQuantity = 5
                }
                PartnershipEntity.new(partnershipId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                    this.selectedPackId = packId
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/validate") {
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
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/validate") {
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
            insertMockCompany(companyId)
            transaction {
                PartnershipEntity.new(partnershipId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/decline") {
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
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.delete("/events/$eventId/companies/$companyId/partnership/$partnershipId/decline") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
