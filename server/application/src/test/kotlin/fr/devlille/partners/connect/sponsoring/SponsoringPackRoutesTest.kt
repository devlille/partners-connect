package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SponsoringPackRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns empty list when no packs exist`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.get("/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `POST creates a new pack`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val body = CreateSponsoringPack(
            name = "Gold",
            price = 3000,
            nbTickets = 1,
            maxQuantity = 5,
        )

        val response = client.post("/events/$eventId/packs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val createdId = json.decodeFromString<Map<String, String>>(response.bodyAsText())["id"]

        val persisted = transaction {
            SponsoringPackEntity.findById(UUID.fromString(createdId))
        }

        assertNotNull(persisted)
        assertEquals("Gold", persisted.name)
        assertEquals(3000, persisted.basePrice)
        assertEquals(5, persisted.maxQuantity)
    }

    @Test
    fun `GET returns all packs with empty options`() = testApplication {
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            repeat(2) {
                insertMockSponsoringPack(
                    eventId = eventId,
                    name = "Pack$it",
                    basePrice = 1000 * (it + 1),
                    maxQuantity = 3 + it,
                )
            }
        }

        val response = client.get("/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<List<SponsoringPack>>(response.bodyAsText())
        assertEquals(2, body.size)
        assertTrue(body.all { it.requiredOptions.isEmpty() && it.optionalOptions.isEmpty() })
    }

    @Test
    fun `GET fails if Accept-Language is missing`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.get("/events/$eventId/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST to attach options adds options to pack`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId1 = UUID.randomUUID()
        val optionId2 = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val option1 = SponsoringOptionEntity.new(optionId1) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = option1
                    this.language = "en"
                    this.name = "Logo"
                    this.description = "Add logo to website"
                }

                val option2 = SponsoringOptionEntity.new(optionId2) {
                    this.eventId = eventId
                    this.price = 150
                }
                OptionTranslationEntity.new {
                    this.option = option2
                    this.language = "en"
                    this.name = "Talk"
                    this.description = "Give a talk"
                }
            }
        }

        val attachRequest = AttachOptionsToPack(
            required = listOf(optionId1.toString()),
            optional = listOf(optionId2.toString()),
        )

        val response = client.post("/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(attachRequest))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val result = client.get("/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val body = result.bodyAsText()
        assertTrue(body.contains("Logo"))
        assertTrue(body.contains("Talk"))
    }

    @Test
    fun `POST to attach options returns 404 if pack does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val attachRequest = AttachOptionsToPack(required = emptyList(), optional = emptyList())

        val response = client.post("/events/$eventId/packs/${UUID.randomUUID()}/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(attachRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST packs options returns 404 when pack does not exist for event`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(UUID.randomUUID().toString()),
                        optional = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue { response.bodyAsText().contains("Pack not found") }
    }

    @Test
    fun `POST packs options returns 400 when same option is in required and optional`() = testApplication {
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = option
                    this.language = "en"
                    this.name = "Logo on website"
                    this.description = "Visible on all pages"
                }
            }
        }

        val response = client.post("/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionId.toString()),
                        optional = listOf(optionId.toString()),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue { response.bodyAsText().contains("cannot be both required and optional") }
    }

    @Test
    fun `POST packs options returns 400 if any option is not linked to the event`() = testApplication {
        val eventId = UUID.randomUUID()
        val otherEventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionValid = UUID.randomUUID()
        val optionInvalid = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockedEvent(otherEventId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val valid = SponsoringOptionEntity.new(optionValid) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = valid
                    this.language = "en"
                    this.name = "Shirt Logo"
                    this.description = null
                }

                val invalid = SponsoringOptionEntity.new(optionInvalid) {
                    this.eventId = otherEventId
                    this.price = 80
                }
                OptionTranslationEntity.new {
                    this.option = invalid
                    this.language = "en"
                    this.name = "Wrong Option"
                    this.description = "Should not be accepted"
                }
            }
        }

        val response = client.post("/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionValid.toString()),
                        optional = listOf(optionInvalid.toString()),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue { response.bodyAsText().contains("Some options do not belong to the event") }
    }

    @Test
    fun `POST packs options returns 400 if option is already attached to pack`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 300
                }
                OptionTranslationEntity.new {
                    this.option = option
                    this.language = "en"
                    this.name = "Booth Placement"
                    this.description = null
                }

                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = true
                }
            }
        }

        val response = client.post("/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionId.toString()),
                        optional = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue { response.bodyAsText().contains("Option already attached to pack") }
    }
}
