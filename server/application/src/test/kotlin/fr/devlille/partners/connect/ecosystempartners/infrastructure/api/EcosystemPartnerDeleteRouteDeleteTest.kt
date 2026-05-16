package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerEmail
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailEntity
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcosystemPartnerDeleteRouteDeleteTest {
    @Test
    fun `DELETE removes the partner and cascades email rows`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedEcosystemPartner(
                    id = partnerId,
                    eventId = eventId,
                    companyId = companyId,
                    categoryId = categoryId,
                )
                insertMockedEcosystemPartnerEmail(ecosystemPartnerId = partnerId, email = "a@example.com")
                insertMockedEcosystemPartnerEmail(ecosystemPartnerId = partnerId, email = "b@example.com")
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        val remainingEmails = transaction {
            EcosystemPartnerEmailEntity.listByEcosystemPartner(partnerId)
        }
        assertTrue(remainingEmails.isEmpty())
    }

    @Test
    fun `DELETE returns 404 when partner belongs to a different event`() = testApplication {
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
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
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

        val response = client.delete("/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
