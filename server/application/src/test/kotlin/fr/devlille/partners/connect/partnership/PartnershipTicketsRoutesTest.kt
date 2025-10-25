package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException
import fr.devlille.partners.connect.internal.insertBilletWebIntegration
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipTicket
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.ProductDetail
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PartnershipTicketsRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns tickets for existing partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-returns-tickets--528"
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
        }

        val response = client.get("/events/$eventSlug/partnership/$partnershipId/tickets")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<List<Ticket>>(response.bodyAsText())
        assertEquals(1, body.size)
        assertEquals(ticketId.toString(), body.first().id)
    }

    @Test
    fun `POST creates tickets for existing partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-creates-tickets-621"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(
                mockNetwork = module {
                    single<HttpClientEngine> {
                        MockEngine {
                            respond(
                                status = HttpStatusCode.OK,
                                content = json.encodeToString(listOf(createOrderWithProducts(nbProducts = 1))),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
                },
            )
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedBilling(eventId, partnershipId)
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventSlug/partnership/$partnershipId/tickets") {
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
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-failed-when-cre-559"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(
                mockNetwork = module {
                    single<HttpClientEngine> {
                        MockEngine {
                            respond(
                                status = HttpStatusCode.OK,
                                content = json.encodeToString(listOf(createOrderWithProducts(nbProducts = 1))),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
                },
            )
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = company.id.value)
            insertMockedBilling(eventId, partnershipId)
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventSlug/partnership/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("No validated pack found for partnership $partnershipId", message)
    }

    @Test
    fun `POST failed when creating ticket when pack has not enough ticket configured`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-failed-when-cre-818"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(
                mockNetwork = module {
                    single<HttpClientEngine> {
                        MockEngine {
                            respond(
                                status = HttpStatusCode.OK,
                                content = json.encodeToString(listOf(createOrderWithProducts(nbProducts = 1))),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
                },
            )
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            val selectedPack = insertMockedSponsoringPack(event = eventId, nbTickets = 0)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedBilling(eventId, partnershipId)
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventSlug/partnership/$partnershipId/tickets") {
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
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-failed-when-cre-316"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(
                mockNetwork = module {
                    single<HttpClientEngine> {
                        MockEngine {
                            respond(
                                status = HttpStatusCode.OK,
                                content = json.encodeToString(listOf(createOrderWithProducts(nbProducts = 1))),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
                },
            )
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedBilling(eventId, partnershipId, status = InvoiceStatus.PENDING)
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventSlug/partnership/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Invoice status PENDING is not PAID", message)
    }

    @Test
    fun `PUT updates an existing ticket`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-updates-an-exist-17"
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleMocked(
                mockNetwork = module {
                    single<HttpClientEngine> {
                        MockEngine {
                            respond(
                                status = HttpStatusCode.OK,
                                content = json.encodeToString(listOf(createOrderWithProducts(nbProducts = 1))),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                    }
                },
            )
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockedPartnershipTicket(ticketId = ticketId, partnershipId = partnershipId)
            insertMockedBilling(eventId, partnershipId)
            insertBilletWebIntegration(eventId = eventId)
        }

        val response = client.put("/events/$eventSlug/partnership/$partnershipId/tickets/$ticketId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TicketData(firstName = "Jeanne", lastName = "Doe")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString(Ticket.serializer(), response.bodyAsText())
        assertEquals(ticketId.toString(), body.id)
    }

    @Test
    fun `GET returns 204 if no ticket generated for the partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-returns-404-if-p-518"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
        }

        val response = client.get("/events/$eventSlug/partnership/$partnershipId/tickets")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `PUT returns 404 if ticket does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-returns-404-if-t-774"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            val company = insertMockedCompany()
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = company.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.put("/events/$eventSlug/partnership/$partnershipId/tickets/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TicketData(firstName = "Jeanne", lastName = "Doe")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private fun createOrderWithProducts(nbProducts: Int): CreateOrderResponseItem {
        return CreateOrderResponseItem(
            id = UUID.randomUUID().toString(),
            productsDetails = (0.until(nbProducts)).map {
                ProductDetail(
                    id = UUID.randomUUID().toString(),
                    extId = UUID.randomUUID().toString(),
                    productDownload = "https://example.com/download/$it",
                )
            },
        )
    }
}
