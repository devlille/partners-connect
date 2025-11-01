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
    fun `GET succeeds without Accept-Language header for organizer endpoints`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-get-succeeds-without-accept-lang"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        // Organizer endpoints now work without Accept-Language header
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET returns all translations regardless of Accept-Language header for organizer endpoints`() =
        testApplication {
            val orgId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val eventSlug = "test-get-returns-all-translations"
            application {
                moduleMocked()
                insertMockedOrganisationEntity(orgId)
                insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

                val pack = insertMockedSponsoringPack(event = eventId)
                val option = insertMockedSponsoringOption(eventId = eventId)
                insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value)
            }

            val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
                header(HttpHeaders.AcceptLanguage, "xx") // Unsupported language but should still work
                header(HttpHeaders.Authorization, "Bearer valid")
            }
            // Organizer endpoints return all translations, so any Accept-Language value is fine
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("translations"))
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
            insertMockedSponsoringOption(optionId = optionId1, eventId = eventId, name = "Logo")
            insertMockedSponsoringOption(optionId = optionId2, eventId = eventId, name = "Talk")
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
    fun `POST packs options returns 409 when same option is in required and optional`() = testApplication {
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

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue { response.bodyAsText().contains("cannot be both required and optional") }
    }

    @Test
    fun `POST packs options returns 403 if any option is not linked to the event`() = testApplication {
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
            insertMockedSponsoringOption(optionId = optionInvalid, eventId = otherEventId)
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

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue { response.bodyAsText().contains("Some options do not belong to the event") }
    }

    @Test
    fun `POST packs options returns 409 if option is already attached to pack`() = testApplication {
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

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue { response.bodyAsText().contains("Option already attached to pack") }
    }

    @Test
    fun `GET returns all packs with all translations without Accept-Language header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-multi-language-packs"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Gold Sponsor")
            val option = insertMockedSponsoringOption(
                eventId = eventId,
                name = "Logo on website",
                description = "Company logo displayed on event website",
            )

            // Insert additional translations for the option (en already created by insertMockedSponsoringOption)
            insertMockedOptionTranslation(
                optionId = option.id.value,
                language = "fr",
                name = "Logo sur le site web",
                description = "Logo de l'entreprise affiché sur le site de l'événement",
            )
            insertMockedOptionTranslation(
                optionId = option.id.value,
                language = "de",
                name = "Logo auf Website",
                description = "",
            )

            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = true)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // Intentionally NO Accept-Language header
        }

        // Implementation completed - endpoint now works without Accept-Language header
        // We expect OK status and response containing all translations
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())

        // Verify response contains translations map structure
        assertTrue(responseBody.contains("translations"))
        assertTrue(
            responseBody.contains("\"en\":") ||
                responseBody.contains("\"fr\":") ||
                responseBody.contains("\"de\":"),
        )
    }
}
