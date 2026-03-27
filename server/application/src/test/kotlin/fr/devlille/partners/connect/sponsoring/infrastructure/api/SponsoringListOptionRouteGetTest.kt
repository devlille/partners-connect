package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsoringListOptionRouteGetTest {
    @Test
    fun `GET returns empty list when no options exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns wrapper with partnership_count 0 when option has no partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, items.size)
        val item = items[0].jsonObject
        assertTrue(item.containsKey("option"))
        assertTrue(item.containsKey("partnership_count"))
        assertEquals(0, item["partnership_count"]!!.jsonPrimitive.int)
        assertTrue(item["option"]!!.jsonObject.containsKey("translations"))
    }

    @Test
    fun `GET returns correct partnership_count for validated partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId)
                insertMockedCompany(companyId1)
                insertMockedCompany(companyId2)
                val partnership1 = insertMockedPartnership(
                    eventId = eventId,
                    companyId = companyId1,
                    selectedPackId = packId,
                    validatedAt = now,
                )
                insertMockedOptionPartnership(
                    partnershipId = partnership1.id.value,
                    packId = packId,
                    optionId = optionId,
                )
                val partnership2 = insertMockedPartnership(
                    eventId = eventId,
                    companyId = companyId2,
                    selectedPackId = packId,
                    validatedAt = now,
                )
                insertMockedOptionPartnership(
                    partnershipId = partnership2.id.value,
                    packId = packId,
                    optionId = optionId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, items.size)
        assertEquals(2, items[0].jsonObject["partnership_count"]!!.jsonPrimitive.int)
    }

    @Test
    fun `GET does not count declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    declinedAt = now,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, items.size)
        assertEquals(0, items[0].jsonObject["partnership_count"]!!.jsonPrimitive.int)
    }

    @Test
    fun `GET returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
