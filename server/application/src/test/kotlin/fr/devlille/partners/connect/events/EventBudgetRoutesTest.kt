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
}
