package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.factories.insertMockedEcosystemPartnerCategory
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EcosystemPartnerListRouteGetTest {
    @Test
    fun `GET lists all non-declined partners by default`() = testApplication {
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
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
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
                    declinedAt = LocalDateTime(2026, 1, 2, 0, 0),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/ecosystem-partners") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun `GET filtered by category_id`() = testApplication {
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
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(catA, eventId = eventId, name = "Media")
                insertMockedEcosystemPartnerCategory(catB, eventId = eventId, name = "Press")
                insertMockedCompany(company1)
                insertMockedCompany(company2)
                insertMockedEcosystemPartner(eventId = eventId, companyId = company1, categoryId = catA)
                insertMockedEcosystemPartner(eventId = eventId, companyId = company2, categoryId = catB)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/ecosystem-partners?filter[category_id]=$catA") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
    }

    @Test
    fun `GET filtered by validated=true`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val company1 = UUID.randomUUID()
        val company2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedEcosystemPartnerCategory(categoryId, eventId = eventId, name = "Media")
                insertMockedCompany(company1)
                insertMockedCompany(company2)
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
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/ecosystem-partners?filter[validated]=true") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, body.size)
    }
}
