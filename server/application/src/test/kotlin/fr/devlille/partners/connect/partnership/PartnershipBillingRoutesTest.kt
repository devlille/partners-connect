package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.companies.domain.Contact
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
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
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipBillingRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleBillingInput(name: String? = "DevLille SAS", po: String? = "PO1234") =
        CompanyBillingData(
            name = name,
            po = po,
            contact = Contact(
                firstName = "Jean",
                lastName = "Dupont",
                email = "jean.dupont@example.com",
            ),
        )

    @Test
    fun `GET returns billing for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val partnership = insertMockPartnership(
                id = partnershipId,
                event = insertMockedEvent(eventId),
                company = insertMockCompany(companyId),
            )
            transaction {
                BillingEntity.new {
                    this.event = partnership.event
                    this.partnership = partnership
                    this.name = "DevLille SAS"
                    this.contactFirstName = "Jean"
                    this.contactLastName = "Dupont"
                    this.contactEmail = "jean.dupont@example.com"
                    this.po = "PO1234"
                    this.status = InvoiceStatus.SENT
                }
            }
        }

        val response = client.get("/events/$eventId/partnership/$partnershipId/billing")

        assertEquals(HttpStatusCode.Companion.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("DevLille SAS"))
        assertTrue(body.contains("jean.dupont@example.com"))
    }

    @Test
    fun `GET returns 404 if billing does not exist for company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
        }

        val response = client.get("/events/$eventId/partnership/$partnershipId/billing")

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Billing not found"))
    }

    @Test
    fun `POST creates billing for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
            )
            transaction {
                IntegrationsTable.insertAndGetId {
                    it[this.eventId] = eventId
                    it[this.provider] = IntegrationProvider.QONTO
                    it[this.usage] = IntegrationUsage.BILLING
                }
            }
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), sampleBillingInput()))
        }

        println(response.bodyAsText())
        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("id"))
    }

    @Test
    fun `PUT updates billing if it exists`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val partnership = insertMockPartnership(
                id = partnershipId,
                event = insertMockedEvent(eventId),
                company = insertMockCompany(companyId),
            )
            transaction {
                IntegrationsTable.insertAndGetId {
                    it[this.eventId] = eventId
                    it[this.provider] = IntegrationProvider.QONTO
                    it[this.usage] = IntegrationUsage.BILLING
                }
                BillingEntity.new {
                    this.event = partnership.event
                    this.partnership = partnership
                    this.name = "Old Name"
                    this.contactFirstName = "Old"
                    this.contactLastName = "Name"
                    this.contactEmail = "old@example.com"
                    this.po = "OLDPO"
                    this.status = InvoiceStatus.PENDING
                }
            }
        }

        val response = client.put("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    CompanyBillingData.serializer(),
                    sampleBillingInput(name = "Updated SAS", po = "NEWPO"),
                ),
            )
        }

        assertEquals(HttpStatusCode.Companion.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST fails if partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), sampleBillingInput()))
        }

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Partnership not found"))
    }

    @Test
    fun `PUT fails if partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.put("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), sampleBillingInput()))
        }

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Partnership not found"))
    }
}
