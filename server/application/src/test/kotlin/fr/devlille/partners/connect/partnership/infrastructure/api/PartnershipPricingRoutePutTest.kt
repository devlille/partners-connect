package fr.devlille.partners.connect.partnership.infrastructure.api

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
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for PUT /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/pricing
 *
 * Covers: US1 (pack price override) and US2 (option price overrides)
 * Validates all HTTP status codes without asserting full business logic.
 */
class PartnershipPricingRoutePutTest {
    // --- US1: Pack price overrides ---

    @Test
    fun `PUT returns 200 when setting a valid pack price override`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"pack_price_override": 150000}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("pack_price_override"))
    }

    @Test
    fun `PUT returns 200 when clearing a pack price override with null`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    packPriceOverride = 100000,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"pack_price_override": null}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 200 for a pending partnership (status must not block override)`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                // Pending = selectedPack set but not validatedAt
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        // An empty body is valid — pack_price_override absent means null → clears any existing override
        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 400 for negative pack price`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"pack_price_override": -1}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 409 when setting pack override on partnership with no validated pack`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                // Partnership has no pack at all
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"pack_price_override": 80000}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `PUT returns 401 without Authorization header`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user has no permission for the org`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val otherOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                // userId only has permission for otherOrgId, not orgId
                insertMockedOrganisationEntity(orgId)
                insertMockedOrganisationEntity(otherOrgId)
                insertMockedOrgaPermission(otherOrgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 for non-existent event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 for non-existent partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // --- US2: Option price overrides ---

    @Test
    fun `PUT returns 200 OK for valid single option override`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedOptionPartnership(partnershipId, packId, optionId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                """{"options_price_overrides": [{"id": "$optionId", "price_override": 5000}]}""",
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 200 OK when overriding multiple options simultaneously`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val optionId1 = UUID.randomUUID()
        val optionId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId1, eventId)
                insertMockedSponsoringOption(optionId2, eventId)
                insertMockedPackOptions(packId, optionId1, required = false)
                insertMockedPackOptions(packId, optionId2, required = false)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedOptionPartnership(partnershipId, packId, optionId1)
                insertMockedOptionPartnership(partnershipId, packId, optionId2)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                """{"options_price_overrides": [
                    {"id": "$optionId1", "price_override": 5000},
                    {"id": "$optionId2", "price_override": 7500}
                ]}""",
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 200 OK when clearing an existing option override`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                // Pre-seed an existing override
                insertMockedOptionPartnership(partnershipId, packId, optionId, priceOverride = 5000)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                """{"options_price_overrides": [{"id": "$optionId", "price_override": null}]}""",
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 404 for an option not associated with the partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val nonExistentOptionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                // No option entity inserted — nonExistentOptionId is not associated
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                """{"options_price_overrides": [{"id": "$nonExistentOptionId", "price_override": 1000}]}""",
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
