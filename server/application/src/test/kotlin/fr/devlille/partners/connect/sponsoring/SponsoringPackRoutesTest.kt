package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
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

        val response = client.post("/events/$eventId/packs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val createdId = json.decodeFromString<Map<String, String>>(response.bodyAsText())["id"]

        val persisted = transaction {
            SponsoringPackEntity.findById(UUID.fromString(createdId))
        }

        assertNotNull(persisted)
        assertEquals("Silver", persisted.name)
        assertEquals(2000, persisted.basePrice)
        assertEquals(10, persisted.maxQuantity)
    }

    @Test
    fun `GET returns all packs with empty options`() = testApplication {
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            repeat(2) {
                insertMockedSponsoringPack(
                    event = eventId,
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
    fun `GET fails if Accept-Language is not supported`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
            val pack = insertMockedSponsoringPack(event = eventId)
            val option = insertMockedSponsoringOption(eventId = eventId)
            insertMockedOptionTranslation(optionId = option.id.value)
            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value)
        }

        val response = client.get("/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "xx")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Translation not found for option"))
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
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionId1, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId1, name = "Logo")
            insertMockedSponsoringOption(optionId = optionId2, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId2, name = "Talk")
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
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId)
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
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionValid, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionValid)
            insertMockedSponsoringOption(optionId = optionInvalid, eventId = otherEventId)
            insertMockedOptionTranslation(optionId = optionInvalid)
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
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId)
            insertMockedPackOptions(packId, optionId)
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
