package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-returns-empty-li-466"
        val testOrgSlug = "test-org-empty"
        val testEventSlug = "test-event-empty"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug, representativeUser = admin)
            insertMockedOrgaPermission(orgId = orgId, user = admin)

            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event Empty")
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `POST creates a new pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-creates-a-new-p-838"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-returns-all-pack-79"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            repeat(2) {
                insertMockedSponsoringPack(
                    event = eventId,
                    name = "Pack$it",
                    basePrice = 1000 * (it + 1),
                    maxQuantity = 3 + it,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-fails-if-accept--118"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET fails if Accept-Language is not supported`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-fails-if-accept--193"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId)
            val option = insertMockedSponsoringOption(eventId = eventId)
            insertMockedOptionTranslation(optionId = option.id.value)
            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.AcceptLanguage, "xx")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Translation not found for option"))
    }

    @Test
    fun `POST to attach options adds options to pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-to-attach-optio-232"
        val packId = UUID.randomUUID()
        val optionId1 = UUID.randomUUID()
        val optionId2 = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

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

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(attachRequest))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val result = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val body = result.bodyAsText()
        assertTrue(body.contains("Logo"))
        assertTrue(body.contains("Talk"))
    }

    @Test
    fun `POST to attach options returns 404 if pack does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-to-attach-optio-522"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val attachRequest = AttachOptionsToPack(required = emptyList(), optional = emptyList())

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs/${UUID.randomUUID()}/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(attachRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST packs options returns 404 when pack does not exist for event`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-packs-options-r-341"
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-packs-options-r-541"
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-packs-options-r-864"
        val orgSlug = orgId.toString()
        val otherEventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionValid = UUID.randomUUID()
        val optionInvalid = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug)
            val event = insertMockedEventWithAdminUser(eventId, orgId = orgId, eventSlug)

            insertMockedEventWithOrga(otherEventId, organisation = org)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionValid, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionValid)
            insertMockedSponsoringOption(optionId = optionInvalid, eventId = otherEventId)
            insertMockedOptionTranslation(optionId = optionInvalid)
        }

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/packs/$packId/options") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-post-packs-options-r-522"
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedOptionTranslation(optionId = optionId)
            insertMockedPackOptions(packId, optionId)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
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
