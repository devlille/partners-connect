package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException
import fr.devlille.partners.connect.internal.insertBilletWebIntegration
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipTicket
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PartnershipTicketCreationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates tickets for existing partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                nbProductsForTickets = 1,
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringOption(
                    optionId = optionId,
                    eventId = eventId,
                    optionType = OptionType.TYPED_NUMBER,
                )
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedOptionPartnership(
                    partnershipId = partnershipId,
                    packId = packId,
                    optionId = optionId,
                )
                insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
                insertMockedBilling(eventId, partnershipId)
                insertBilletWebIntegration(eventId = eventId)
            }
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.decodeFromString(TicketOrder.serializer(), response.bodyAsText())
        assertEquals(tickets.size, body.tickets.size)
        val ticket = transaction { PartnershipTicketEntity.findById(body.tickets.first().id) }
        assertNotNull(ticket)
    }

    @Test
    fun `POST failed when creating ticket because no pack has been validated`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                nbProductsForTickets = 1,
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringOption(
                    optionId = optionId,
                    eventId = eventId,
                    optionType = OptionType.TYPED_NUMBER,
                )
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = null,
                )
                insertMockedOptionPartnership(
                    partnershipId = partnershipId,
                    packId = packId,
                    optionId = optionId,
                )
                insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
                insertMockedBilling(eventId, partnershipId)
                insertBilletWebIntegration(eventId = eventId)
            }
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("No validated pack found for partnership $partnershipId", message)
    }

    @Test
    fun `POST failed when creating ticket when pack has not enough ticket configured`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                nbProductsForTickets = 1,
            )
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
                insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
                insertMockedBilling(eventId, partnershipId)
                insertBilletWebIntegration(eventId = eventId)
            }
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        val expected = "Not enough tickets in the validated pack: 0 available, ${tickets.size} requested"
        assertEquals(expected, message)
    }

    @Test
    fun `POST failed when creating ticket because billing isn't paid`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleSharedDb(
                userId = userId,
                nbProductsForTickets = 1,
            )
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringOption(
                    optionId = optionId,
                    eventId = eventId,
                    optionType = OptionType.TYPED_NUMBER,
                )
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedOptionPartnership(
                    partnershipId = partnershipId,
                    packId = packId,
                    optionId = optionId,
                )
                insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
                insertMockedBilling(eventId, partnershipId, status = InvoiceStatus.PENDING)
                insertBilletWebIntegration(eventId = eventId)
            }
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Invoice status PENDING is not PAID", message)
    }
}
