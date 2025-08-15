package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.insertQontoIntegration
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.createCompanyBillingData
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipBillingRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns billing for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertMockedBilling(eventId, partnershipId, name = "DevLille SAS", status = InvoiceStatus.SENT)
        }

        val response = client.get("/events/$eventId/partnership/$partnershipId/billing")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("DevLille SAS"))
    }

    @Test
    fun `GET returns 404 if billing does not exist for company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
            insertMockedCompany(companyId)
        }

        val response = client.get("/events/$eventId/partnership/$partnershipId/billing")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Billing not found"))
    }

    @Test
    fun `POST creates billing for existing company`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertQontoIntegration(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), createCompanyBillingData()))
        }

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
            insertMockedEventWithOrga(eventId)
            insertQontoIntegration(eventId)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertMockedBilling(eventId, partnershipId, status = InvoiceStatus.PENDING)
        }

        val response = client.put("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    CompanyBillingData.serializer(),
                    createCompanyBillingData(name = "Updated SAS", po = "NEWPO"),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST fails if partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), createCompanyBillingData()))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Partnership not found"))
    }

    @Test
    fun `PUT fails if partnership not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId)
        }

        val response = client.put("/events/$eventId/partnership/$partnershipId/billing") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CompanyBillingData.serializer(), createCompanyBillingData()))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Partnership not found"))
    }
}
