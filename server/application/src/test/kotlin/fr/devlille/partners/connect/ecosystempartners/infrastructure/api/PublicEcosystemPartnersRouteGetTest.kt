package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicEcosystemPartnersRouteGetTest {
    @Test
    fun `GET returns only validated non-declined partners grouped by category`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val company1 = UUID.randomUUID()
        val company2 = UUID.randomUUID()
        val company3 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedCompany(company1)
                insertMockedCompany(company2)
                insertMockedCompany(company3)
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = company1,
                    categoryId = categoryId,
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                )
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = company2,
                    categoryId = categoryId,
                )
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = company3,
                    categoryId = categoryId,
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                    declinedAt = LocalDateTime(2026, 1, 2, 0, 0),
                )
            }
        }

        val response = client.get("/events/$eventId/ecosystem-partners")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
        val partners = body[0].jsonObject["partners"]!!.jsonArray
        assertEquals(1, partners.size)
    }

    @Test
    fun `GET returns groups in displayOrder then name`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catA = UUID.randomUUID()
        val catB = UUID.randomUUID()
        val company1 = UUID.randomUUID()
        val company2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                // catA displayOrder=2 (Alpha), catB displayOrder=1 (Beta) -> Beta should come first
                insertMockedEcosystemPartnerCategory(catA, eventId = eventId, name = "Alpha", displayOrder = 2)
                insertMockedEcosystemPartnerCategory(catB, eventId = eventId, name = "Beta", displayOrder = 1)
                insertMockedCompany(company1)
                insertMockedCompany(company2)
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = company1,
                    categoryId = catA,
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                )
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = company2,
                    categoryId = catB,
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                )
            }
        }

        val response = client.get("/events/$eventId/ecosystem-partners")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
        assertEquals("Beta", body[0].jsonObject["category"]!!.jsonPrimitive.content)
        assertEquals("Alpha", body[1].jsonObject["category"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET returns empty list for an event with no validated partners`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/events/$eventId/ecosystem-partners")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(0, body.size)
    }
}
