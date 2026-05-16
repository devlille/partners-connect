package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EcosystemPartnerRouteGetTest {
    @Test
    fun `GET returns 200 with the partner`() = testApplication {
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
            }
        }

        val response = client.get("/events/$eventId/ecosystem-partners/$partnerId")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET returns 404 when partner belongs to a different event`() = testApplication {
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

        val response = client.get("/events/$eventId/ecosystem-partners/$partnerId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
