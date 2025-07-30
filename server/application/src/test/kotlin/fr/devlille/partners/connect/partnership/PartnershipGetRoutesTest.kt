package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipGetRoutesTest {
    @Test
    fun `GET returns partnership for company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            transaction {
                CompanyEntity.new(companyId) {
                    name = "Test Company"
                    siteUrl = "https://example.com"
                }
                SponsoringPackEntity.new(packId) {
                    this.eventId = eventId
                    this.name = "Test Pack"
                    this.basePrice = 1000
                    this.maxQuantity = 1
                }
                PartnershipEntity.new(partnerId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.selectedPackId = packId
                    this.language = "en"
                }
            }
        }

        val response = client.get("/events/$eventId/companies/$companyId/partnership")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET returns 404 when partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            transaction {
                CompanyEntity.new(companyId) {
                    name = "Test Company"
                    siteUrl = "https://example.com"
                }
            }
        }

        val response = client.get("/events/$eventId/companies/$companyId/partnership")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}