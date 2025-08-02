package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.companies.domain.Contact
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
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

class PartnershipInvoiceRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleInvoiceInput(name: String? = "DevLille SAS", po: String? = "PO1234") =
        CompanyInvoice(
            name = name,
            siret = "12345678900011",
            vat = "FR123456789",
            po = po,
            address = Address(
                address = "42 rue de la République",
                city = "Lille",
                zipCode = "59000",
                country = "FR",
            ),
            contact = Contact(
                firstName = "Jean",
                lastName = "Dupont",
                email = "jean.dupont@example.com",
            ),
        )

    @Test
    fun `GET returns invoice for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val event = insertMockedEvent(eventId)
            val company = insertMockCompany(companyId)
            transaction {
                InvoiceEntity.new {
                    this.event = event
                    this.company = company
                    this.name = "DevLille SAS"
                    this.contactFirstName = "Jean"
                    this.contactLastName = "Dupont"
                    this.contactEmail = "jean.dupont@example.com"
                    this.address = "42 rue de la République"
                    this.city = "Lille"
                    this.zipCode = "59000"
                    this.country = "FR"
                    this.siret = "12345678900011"
                    this.vat = "FR123456789"
                    this.po = "PO1234"
                    this.status = InvoiceStatus.SENT
                }
            }
        }

        val response = client.get("/events/$eventId/companies/$companyId/partnership/$partnershipId/invoice")

        assertEquals(HttpStatusCode.Companion.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("DevLille SAS"))
        assertTrue(body.contains("jean.dupont@example.com"))
    }

    @Test
    fun `GET returns 404 if invoice does not exist for company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
        }

        val response = client.get("/events/$eventId/companies/$companyId/partnership/$partnershipId/invoice")

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Invoice not found"))
    }

    @Test
    fun `POST creates invoice for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            transaction {
                PartnershipEntity.new(partnershipId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
                IntegrationsTable.insertAndGetId {
                    it[this.eventId] = eventId
                    it[this.provider] = IntegrationProvider.QONTO
                    it[this.usage] = IntegrationUsage.INVOICE
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/invoice") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyInvoice.serializer(), sampleInvoiceInput()))
        }

        assertEquals(HttpStatusCode.Companion.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("id"))
    }

    @Test
    fun `PUT updates invoice if it exists`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val event = insertMockedEvent(eventId)
            val company = insertMockCompany(companyId)
            transaction {
                PartnershipEntity.new(partnershipId) {
                    this.eventId = eventId
                    this.companyId = companyId
                    this.language = "en"
                }
                IntegrationsTable.insertAndGetId {
                    it[this.eventId] = eventId
                    it[this.provider] = IntegrationProvider.QONTO
                    it[this.usage] = IntegrationUsage.INVOICE
                }
                InvoiceEntity.new {
                    this.event = event
                    this.company = company
                    this.name = "Old Name"
                    this.contactFirstName = "Old"
                    this.contactLastName = "Name"
                    this.contactEmail = "old@example.com"
                    this.address = "Old street"
                    this.city = "Oldtown"
                    this.zipCode = "00000"
                    this.country = "FR"
                    this.siret = "00000000000000"
                    this.vat = "FR000000000"
                    this.po = "OLDPO"
                    this.status = InvoiceStatus.PENDING
                }
            }
        }

        val response = client.put("/events/$eventId/companies/$companyId/partnership/$partnershipId/invoice") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    CompanyInvoice.serializer(),
                    sampleInvoiceInput(name = "Updated SAS", po = "NEWPO"),
                ),
            )
        }

        assertEquals(HttpStatusCode.Companion.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST fails if company not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val fakeCompanyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.post("/events/$eventId/companies/$fakeCompanyId/partnership/$partnershipId/invoice") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyInvoice.serializer(), sampleInvoiceInput()))
        }

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Company not found"))
    }

    @Test
    fun `PUT fails if company not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val fakeCompanyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.put("/events/$eventId/companies/$fakeCompanyId/partnership/$partnershipId/invoice") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyInvoice.serializer(), sampleInvoiceInput()))
        }

        assertEquals(HttpStatusCode.Companion.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Company not found"))
    }
}
