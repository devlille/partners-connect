package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EcosystemPartnerDeclineRoutePostTest {
    @Test
    fun `POST decline sets declinedAt and clears validatedAt`() = testApplication {
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
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId/decline",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val entity = EcosystemPartnerEntity.findById(partnerId)
            assertNotNull(entity)
            assertNotNull(entity.declinedAt)
            assertNull(entity.validatedAt)
        }
    }

    @Test
    fun `POST decline clears a previous validation`() = testApplication {
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
                    validatedAt = LocalDateTime(2026, 1, 1, 0, 0),
                )
            }
        }

        val response = client.post(
            "/orgs/$orgId/events/$eventId/ecosystem-partners/$partnerId/decline",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val entity = EcosystemPartnerEntity.findById(partnerId)
            assertNotNull(entity)
            assertNull(entity.validatedAt)
            assertNotNull(entity.declinedAt)
        }
    }
}
