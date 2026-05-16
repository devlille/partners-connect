package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcosystemPartnerRegisterRoutePostTest {
    @Test
    fun `POST creates an ecosystem partner submission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
            }
        }

        val body = RegisterEcosystemPartner(
            companyId = companyId.toString(),
            categoryId = categoryId.toString(),
            language = "en",
            emails = listOf("partner@example.com"),
        )
        val response = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterEcosystemPartner.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"id\""))
    }

    @Test
    fun `POST returns 409 when company already registered for that category`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedEcosystemPartner(
                    eventId = eventId,
                    companyId = companyId,
                    categoryId = categoryId,
                )
            }
        }

        val body = RegisterEcosystemPartner(
            companyId = companyId.toString(),
            categoryId = categoryId.toString(),
            language = "en",
        )
        val response = client.post("/events/$eventId/ecosystem-partners") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterEcosystemPartner.serializer(), body))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
