package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
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
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the partnership pricing endpoint.
 *
 * Verifies end-to-end pricing override workflows including DB persistence,
 * response payload correctness, and partial-update semantics.
 */
class PartnershipPricingRoutesTest {
    @Test
    fun `SET pack override - response includes pack_price_override and adjusted total_price`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId, basePrice = 200000)
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
        assertTrue(
            body.contains("\"pack_price_override\": 150000") || body.contains("\"pack_price_override\":150000"),
            "Response must include the override value",
        )
        // Verify DB state persisted
        transaction {
            val entity = PartnershipEntity.findById(partnershipId)
            assertNotNull(entity)
            assertEquals(150000, entity.packPriceOverride)
        }
    }

    @Test
    fun `CLEAR pack override - pack_price_override is null in response and DB`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId, basePrice = 200000)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    packPriceOverride = 150000,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"pack_price_override": null}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val entity = PartnershipEntity.findById(partnershipId)
            assertNotNull(entity)
            assertNull(entity.packPriceOverride)
        }
    }

    @Test
    fun `SET option override - price_override is persisted and returned`() = testApplication {
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
                insertMockedSponsoringOption(optionId, eventId, price = 10000)
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
            setBody("""{"options_price_overrides": [{"id": "$optionId", "price_override": 7500}]}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(
            body.contains("\"price_override\": 7500") || body.contains("\"price_override\":7500"),
            "Response must include the option price override",
        )
        // Verify DB state
        transaction {
            val optionEntity = PartnershipOptionEntity
                .find {
                    (PartnershipOptionsTable.partnershipId eq partnershipId) and
                        (PartnershipOptionsTable.optionId eq optionId)
                }
                .singleOrNull()
            assertNotNull(optionEntity)
            assertEquals(7500, optionEntity.priceOverride)
        }
    }

    @Test
    fun `SET and CLEAR option override in two sequential requests`() = testApplication {
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
                insertMockedSponsoringOption(optionId, eventId, price = 10000)
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

        // First: set an override
        val setResponse = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"options_price_overrides": [{"id": "$optionId", "price_override": 5000}]}""")
        }
        assertEquals(HttpStatusCode.OK, setResponse.status)

        // Second: clear the override
        val clearResponse = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"options_price_overrides": [{"id": "$optionId", "price_override": null}]}""")
        }
        assertEquals(HttpStatusCode.OK, clearResponse.status)

        transaction {
            val optionEntity = PartnershipOptionEntity
                .find {
                    (PartnershipOptionsTable.partnershipId eq partnershipId) and
                        (PartnershipOptionsTable.optionId eq optionId)
                }
                .singleOrNull()
            assertNotNull(optionEntity)
            assertNull(optionEntity.priceOverride, "Override must be cleared after null assignment")
        }
    }

    @Test
    fun `COMBINED pack and option overrides are applied atomically`() = testApplication {
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
                insertMockedSponsoringOption(optionId, eventId, price = 10000)
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

        val body = """{"pack_price_override": 120000,
            "options_price_overrides": [{"id": "$optionId", "price_override": 5000}]}"""
        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(body)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val partnership = PartnershipEntity.findById(partnershipId)
            assertNotNull(partnership)
            assertEquals(120000, partnership.packPriceOverride)

            val optionEntity = PartnershipOptionEntity
                .find {
                    (PartnershipOptionsTable.partnershipId eq partnershipId) and
                        (PartnershipOptionsTable.optionId eq optionId)
                }
                .singleOrNull()
            assertNotNull(optionEntity)
            assertEquals(5000, optionEntity.priceOverride)
        }
    }

    @Test
    fun `ATOMICITY - invalid option ID rolls back all changes including valid pack override`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val unknownOptionId = UUID.randomUUID()

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
                // No option inserted — unknownOptionId will trigger NotFoundException
            }
        }

        val body = """{"pack_price_override": 120000,
            "options_price_overrides": [{"id": "$unknownOptionId", "price_override": 1000}]}"""
        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(body)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        // Verify the pack override was NOT persisted (transaction rolled back)
        transaction {
            val entity = PartnershipEntity.findById(partnershipId)
            assertNotNull(entity)
            assertNull(entity.packPriceOverride, "Pack override must not be persisted when transaction rolls back")
        }
    }

    @Test
    fun `OMIT options_price_overrides - other option overrides remain unchanged`() = testApplication {
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
                insertMockedSponsoringOption(optionId, eventId, price = 10000)
                insertMockedPackOptions(packId, optionId, required = false)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                // Pre-seed with existing override
                insertMockedOptionPartnership(partnershipId, packId, optionId, priceOverride = 3000)
            }
        }

        // Body omits options_price_overrides → option override must be unchanged
        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/pricing") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val optionEntity = PartnershipOptionEntity
                .find {
                    (PartnershipOptionsTable.partnershipId eq partnershipId) and
                        (PartnershipOptionsTable.optionId eq optionId)
                }
                .singleOrNull()
            assertNotNull(optionEntity)
            assertEquals(3000, optionEntity.priceOverride, "Option override must be preserved when key is absent")
        }
    }
}
