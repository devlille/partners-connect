package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.insertBilletWebIntegration
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockPartnershipTicket
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.insertMockedBilling
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.ProductDetail
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
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
        val partnershipId = UUID.randomUUID()
        val ticketId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnershipTicket(
                ticketId = ticketId,
                partnership = insertMockPartnership(
                    id = partnershipId,
                    event = insertMockedEventWithAdminUser(eventId),
                    company = insertMockedCompany(),
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                ),
            )
        }

        val response = client.get("/events/$eventId/partnerships/$partnershipId/tickets")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<List<Ticket>>(response.bodyAsText())
        assertEquals(1, body.size)
        assertEquals(ticketId.toString(), body.first().id)
    }

    @Test
    fun `POST creates tickets for existing partnership`() = testApplication {
        val eventId = UUID.randomUUID()
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
            insertMockedBilling(
                partnership = insertMockPartnership(
                    id = partnershipId,
                    event = insertMockedEventWithAdminUser(eventId),
                    company = insertMockedCompany(),
                    selectedPack = insertMockSponsoringPack(eventId = eventId),
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                ),
            )
            insertBilletWebIntegration(eventId = eventId)
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
        val eventId = UUID.randomUUID()
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
            insertMockedBilling(
                partnership = insertMockPartnership(
                    id = partnershipId,
                    event = insertMockedEventWithAdminUser(eventId),
                    company = insertMockedCompany(),
                ),
            )
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("No validated pack found for partnership $partnershipId", response.bodyAsText())
    }

    @Test
    fun `POST failed when creating ticket when pack has not enough ticket configured`() = testApplication {
        val eventId = UUID.randomUUID()
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
            insertMockedBilling(
                partnership = insertMockPartnership(
                    id = partnershipId,
                    event = insertMockedEventWithAdminUser(eventId),
                    company = insertMockedCompany(),
                    selectedPack = insertMockSponsoringPack(eventId = eventId, nbTickets = 0),
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                ),
            )
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        val expected = "Not enough tickets in the validated pack: 0 available, ${tickets.size} requested"
        assertEquals(expected, response.bodyAsText())
    }

    @Test
    fun `POST failed when creating ticket because billing isn't paid`() = testApplication {
        val eventId = UUID.randomUUID()
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
            insertMockedBilling(
                partnership = insertMockPartnership(
                    id = partnershipId,
                    event = insertMockedEventWithAdminUser(eventId),
                    company = insertMockedCompany(),
                    selectedPack = insertMockSponsoringPack(eventId = eventId),
                    validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                ),
                status = InvoiceStatus.PENDING,
            )
            insertBilletWebIntegration(eventId = eventId)
        }

        val tickets = listOf(TicketData(firstName = "John", lastName = "Doe"))
        val response = client.post("/events/$eventId/partnerships/$partnershipId/tickets") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(tickets))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Invoice status PENDING is not PAID", response.bodyAsText())
    }

    @Test
    fun `PUT updates an existing ticket`() = testApplication {
        val eventId = UUID.randomUUID()
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
            val partnership = insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockedCompany(),
                selectedPack = insertMockSponsoringPack(eventId = eventId),
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
            insertMockPartnershipTicket(ticketId = ticketId, partnership = partnership)
            insertMockedBilling(partnership = partnership)
            insertBilletWebIntegration(eventId = eventId)
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId/tickets/$ticketId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TicketData(firstName = "Jeanne", lastName = "Doe")))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString(Ticket.serializer(), response.bodyAsText())
        assertEquals(ticketId.toString(), body.id)
    }

    @Test
    fun `GET returns 404 if partnership does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.get("/events/$eventId/partnerships/$partnershipId/tickets")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 if ticket does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockedCompany(),
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId/tickets/${UUID.randomUUID()}") {
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
