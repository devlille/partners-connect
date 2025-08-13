package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipRoutesTest {
    @Test
    fun `POST registers a valid partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockedCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = SponsoringOptionEntity[optionId]
                    this.language = "en"
                    this.name = "Logo"
                    this.description = "Display logo"
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val body = RegisterPartnership(
            packId = packId.toString(),
            companyId = companyId.toString(),
            optionIds = listOf(optionId.toString()),
            language = "en",
            phone = "+33600000000",
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            emails = listOf("partner@example.com"),
        )

        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("id"))
    }

    @Test
    fun `POST returns 404 when event not found`() = testApplication {
        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = "pack",
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/${UUID.randomUUID()}/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company not found`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val body = RegisterPartnership(
            companyId = UUID.randomUUID().toString(),
            packId = "pack",
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when pack not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = UUID.randomUUID().toString(),
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 400 when partnership already exists`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockPartnership(
                event = insertMockedEvent(eventId),
                company = insertMockedCompany(companyId),
                selectedPack = insertMockSponsoringPack(packId, eventId),
            )
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionIds = listOf(),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 400 when option not optional`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockedCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 200
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = true
                }
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionIds = listOf(optionId.toString()),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "en",
        )
        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `POST returns 400 when option has no translation`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockedCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 500
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val body = RegisterPartnership(
            companyId = companyId.toString(),
            packId = packId.toString(),
            optionIds = listOf(optionId.toString()),
            contactName = "John Doe",
            contactRole = "Marketing Manager",
            language = "fr",
        )
        val response = client.post("/events/$eventId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }
}
