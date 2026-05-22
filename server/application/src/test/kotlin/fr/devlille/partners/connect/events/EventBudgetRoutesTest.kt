package fr.devlille.partners.connect.events

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBudgetRoutesTest {
    @Test
    fun `returns zero totals and empty packs when event has no partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("EUR", body["currency"]!!.jsonPrimitive.content)
        val totals = body["totals"]!!.jsonObject
        assertEquals(0, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, body["packs"]!!.jsonArray.size)
    }

    @Test
    @Suppress("LongMethod")
    fun `sums priceApplied per lifecycle state and excludes declined`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()

        val paidCompanyId = UUID.randomUUID()
        val validatedCompanyId = UUID.randomUUID()
        val pendingCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        val paidPartnershipId = UUID.randomUUID()
        val validatedPartnershipId = UUID.randomUUID()
        val pendingPartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = goldPackId,
                    eventId = eventId,
                    name = "Gold",
                    basePrice = 1000,
                )

                fr.devlille.partners.connect.companies.factories.insertMockedCompany(paidCompanyId, name = "Acme")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(validatedCompanyId, name = "Beta")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(pendingCompanyId, name = "Gamma")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(declinedCompanyId, name = "Delta")

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = paidPartnershipId,
                    eventId = eventId,
                    companyId = paidCompanyId,
                    selectedPackId = goldPackId,
                )
                fr.devlille.partners.connect.partnership.factories.insertMockedBilling(
                    eventId = eventId,
                    partnershipId = paidPartnershipId,
                    status = fr.devlille.partners.connect.partnership.domain.InvoiceStatus.PAID,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = validatedPartnershipId,
                    eventId = eventId,
                    companyId = validatedCompanyId,
                    selectedPackId = goldPackId,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = pendingPartnershipId,
                    eventId = eventId,
                    companyId = pendingCompanyId,
                    selectedPackId = goldPackId,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = goldPackId,
                    declinedAt = kotlinx.datetime.LocalDateTime.parse("2024-01-01T00:00:00"),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val totals = Json.parseToJsonElement(response.bodyAsText()).jsonObject["totals"]!!.jsonObject
        assertEquals(1000, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(2000, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(3000, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    @Suppress("LongMethod")
    fun `groups non-declined partnerships by pack with company name price applied and status`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()
        val silverPackId = UUID.randomUUID()

        val acmeCompanyId = UUID.randomUUID()
        val betaCompanyId = UUID.randomUUID()
        val gammaCompanyId = UUID.randomUUID()
        val deltaCompanyId = UUID.randomUUID()

        val acmePartnershipId = UUID.randomUUID()
        val betaPartnershipId = UUID.randomUUID()
        val gammaPartnershipId = UUID.randomUUID()
        val deltaPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)

                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = goldPackId,
                    eventId = eventId,
                    name = "Gold",
                    basePrice = 1000,
                )
                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = silverPackId,
                    eventId = eventId,
                    name = "Silver",
                    basePrice = 500,
                )

                fr.devlille.partners.connect.companies.factories.insertMockedCompany(acmeCompanyId, name = "Acme")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(betaCompanyId, name = "Beta")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(gammaCompanyId, name = "Gamma")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(deltaCompanyId, name = "Delta")

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = acmePartnershipId,
                    eventId = eventId,
                    companyId = acmeCompanyId,
                    selectedPackId = goldPackId,
                ).also { it.packPriceOverride = 1500 }
                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = betaPartnershipId,
                    eventId = eventId,
                    companyId = betaCompanyId,
                    selectedPackId = goldPackId,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = gammaPartnershipId,
                    eventId = eventId,
                    companyId = gammaCompanyId,
                    selectedPackId = silverPackId,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = deltaPartnershipId,
                    eventId = eventId,
                    companyId = deltaCompanyId,
                    selectedPackId = goldPackId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val packs = body["packs"]!!.jsonArray
        assertEquals(2, packs.size)
        assertEquals("Gold", packs[0].jsonObject["pack_name"]!!.jsonPrimitive.content)
        assertEquals("Silver", packs[1].jsonObject["pack_name"]!!.jsonPrimitive.content)

        val gold = packs[0].jsonObject
        assertEquals(1000, gold["base_price"]!!.jsonPrimitive.content.toInt())
        val goldTotals = gold["totals"]!!.jsonObject
        assertEquals(0, goldTotals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(2500, goldTotals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(2500, goldTotals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(3500, goldTotals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, goldTotals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
        val goldPartnerships = gold["partnerships"]!!.jsonArray
        assertEquals(3, goldPartnerships.size)
        assertEquals("Acme", goldPartnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1500, goldPartnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("validated", goldPartnerships[0].jsonObject["status"]!!.jsonPrimitive.content)
        assertEquals("Beta", goldPartnerships[1].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1000, goldPartnerships[1].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("validated", goldPartnerships[1].jsonObject["status"]!!.jsonPrimitive.content)
        assertEquals("Delta", goldPartnerships[2].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1000, goldPartnerships[2].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("submitted", goldPartnerships[2].jsonObject["status"]!!.jsonPrimitive.content)

        val silver = packs[1].jsonObject
        val silverTotals = silver["totals"]!!.jsonObject
        assertEquals(0, silverTotals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(500, silverTotals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(500, silverTotals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(500, silverTotals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, silverTotals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
        val silverPartnerships = silver["partnerships"]!!.jsonArray
        assertEquals(1, silverPartnerships.size)
        assertEquals("Gamma", silverPartnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(500, silverPartnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("validated", silverPartnerships[0].jsonObject["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun `returns 404 when event slug does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/events/does-not-exist/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `returns 401 when no auth header is provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns 401 when user is not member of the owning organisation`() = testApplication {
        val ownerUserId = UUID.randomUUID()
        val outsiderUserId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = outsiderUserId)
            transaction {
                insertMockedUser(ownerUserId)
                insertMockedUser(outsiderUserId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = ownerUserId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
