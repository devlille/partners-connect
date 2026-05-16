package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
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

class EcosystemPartnerCategoryRouteDeleteTest {
    @Test
    fun `DELETE removes an unused category`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catId, eventId = eventId, name = "Media")
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE returns 409 when an ecosystem partner uses the category`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val catId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catId, eventId = eventId, name = "Media")
                insertMockedCompany(companyId)
                insertMockedEcosystemPartner(
                    id = partnerId,
                    eventId = eventId,
                    companyId = companyId,
                    categoryId = catId,
                )
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `DELETE returns 404 when category belongs to a different event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val otherEventId = UUID.randomUUID()
        val catId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedFutureEvent(otherEventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catId, eventId = otherEventId, name = "Media")
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/ecosystem-partner-categories/$catId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
