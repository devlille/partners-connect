package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership
import fr.devlille.partners.connect.sponsoring.domain.OptionType
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
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBudgetLifecycleRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `full lifecycle - paid validated unvalidated declined with option overrides`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

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

                insertMockedSponsoringPack(id = goldPackId, eventId = eventId, name = "Gold", basePrice = 1000)
                insertMockedSponsoringOption(
                    optionId = optionId, eventId = eventId, optionType = OptionType.TEXT, price = 200,
                )
                insertMockedPackOptions(packId = goldPackId, optionId = optionId, required = false)

                insertMockedCompany(paidCompanyId, name = "Acme")
                insertMockedCompany(validatedCompanyId, name = "Beta")
                insertMockedCompany(pendingCompanyId, name = "Gamma")
                insertMockedCompany(declinedCompanyId, name = "Delta")

                insertMockedValidatedPartnership(
                    id = paidPartnershipId, eventId = eventId, companyId = paidCompanyId, selectedPackId = goldPackId,
                )
                insertMockedOptionPartnership(
                    partnershipId = paidPartnershipId,
                    packId = goldPackId,
                    optionId = optionId,
                    priceOverride = 50,
                )
                insertMockedBilling(
                    eventId = eventId,
                    partnershipId = paidPartnershipId,
                    status = InvoiceStatus.PAID,
                )

                insertMockedValidatedPartnership(
                    id = validatedPartnershipId,
                    eventId = eventId,
                    companyId = validatedCompanyId,
                    selectedPackId = goldPackId,
                )

                insertMockedPartnership(
                    id = pendingPartnershipId,
                    eventId = eventId,
                    companyId = pendingCompanyId,
                    selectedPackId = goldPackId,
                )

                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = goldPackId,
                    declinedAt = LocalDateTime.parse("2024-01-01T00:00:00"),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val totals = body["totals"]!!.jsonObject
        assertEquals(1050, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(2050, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(3050, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())

        val packs = body["packs"]!!.jsonArray
        assertEquals(1, packs.size)
        val gold = packs[0].jsonObject
        assertEquals("Gold", gold["pack_name"]!!.jsonPrimitive.content)
        val partnerships = gold["partnerships"]!!.jsonArray
        assertEquals(2, partnerships.size)
        assertEquals("Acme", partnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1050, partnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("Beta", partnerships[1].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1000, partnerships[1].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
    }
}
