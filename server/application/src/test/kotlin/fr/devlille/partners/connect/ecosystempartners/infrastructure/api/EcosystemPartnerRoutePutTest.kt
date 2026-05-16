package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerEmail
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailEntity
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EcosystemPartnerRoutePutTest {
    @Test
    fun `PUT updates emails on an unvalidated partner`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedEcosystemPartner(
                    id = partnerId,
                    eventId = eventId,
                    companyId = companyId,
                    categoryId = categoryId,
                )
                insertMockedEcosystemPartnerEmail(ecosystemPartnerId = partnerId, email = "old@example.com")
            }
        }

        val body = UpdateEcosystemPartner(emails = listOf("new@x.test"))
        val response = client.put("/events/$eventId/ecosystem-partners/$partnerId") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateEcosystemPartner.serializer(), body))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedEmails = transaction {
            EcosystemPartnerEmailEntity.listByEcosystemPartner(partnerId).map { it.email }
        }
        assertEquals(listOf("new@x.test"), updatedEmails)
    }

    @Test
    fun `PUT returns 409 when partner is validated`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedEcosystemPartner(
                    id = partnerId,
                    eventId = eventId,
                    companyId = companyId,
                    categoryId = categoryId,
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                )
            }
        }

        val body = UpdateEcosystemPartner(emails = listOf("new@x.test"))
        val response = client.put("/events/$eventId/ecosystem-partners/$partnerId") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateEcosystemPartner.serializer(), body))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `PUT returns 404 when partner belongs to a different event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val otherEventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedFutureEvent(otherEventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = otherEventId, name = "Media")
                insertMockedEcosystemPartner(
                    id = partnerId,
                    eventId = otherEventId,
                    companyId = companyId,
                    categoryId = categoryId,
                )
            }
        }

        val body = UpdateEcosystemPartner(emails = listOf("new@x.test"))
        val response = client.put("/events/$eventId/ecosystem-partners/$partnerId") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateEcosystemPartner.serializer(), body))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
