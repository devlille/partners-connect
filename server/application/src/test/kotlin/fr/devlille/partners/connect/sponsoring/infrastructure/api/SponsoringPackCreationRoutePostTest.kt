package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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

class SponsoringPackCreationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a new pack`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs") {
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
    fun `POST pack options synchronizes by removing old and adding new options`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionA = UUID.randomUUID()
        val optionB = UUID.randomUUID()
        val optionC = UUID.randomUUID()
        val optionD = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionA, eventId, name = "Logo")
                insertMockedSponsoringOption(optionB, eventId, name = "Booth")
                insertMockedSponsoringOption(optionC, eventId, name = "Talk")
                insertMockedSponsoringOption(optionD, eventId, name = "Article")

                // Attach options A and B initially
                insertMockedPackOptions(packId, optionA, required = true)
                insertMockedPackOptions(packId, optionB, required = false)
            }
        }

        // Sync to options C and D (should replace A and B)
        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionC.toString()),
                        optional = listOf(optionD.toString()),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        // Verify pack now has only C and D
        val verifyResponse = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val body = verifyResponse.bodyAsText()
        assertTrue(body.contains("Talk")) // Option C
        assertTrue(body.contains("Article")) // Option D
        assertTrue(!body.contains("Logo")) // Option A removed
        assertTrue(!body.contains("Booth")) // Option B removed
    }

    @Test
    fun `POST pack options keeps overlapping options and removes non-overlapping`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionA = UUID.randomUUID()
        val optionB = UUID.randomUUID()
        val optionC = UUID.randomUUID()
        val optionD = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionA, eventId, name = "Logo")
                insertMockedSponsoringOption(optionB, eventId, name = "Booth")
                insertMockedSponsoringOption(optionC, eventId, name = "Talk")
                insertMockedSponsoringOption(optionD, eventId, name = "Article")

                // Attach options A, B, C initially
                insertMockedPackOptions(packId, optionA, required = true)
                insertMockedPackOptions(packId, optionB, required = false)
                insertMockedPackOptions(packId, optionC, required = false)
            }
        }

        // Sync to B (optional) and D (required) - should keep B, remove A and C, add D
        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionD.toString()),
                        optional = listOf(optionB.toString()),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        // Verify pack has only B and D
        val verifyResponse = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val body = verifyResponse.bodyAsText()
        assertTrue(body.contains("Booth")) // Option B kept
        assertTrue(body.contains("Article")) // Option D added
        assertTrue(!body.contains("Logo")) // Option A removed
        assertTrue(!body.contains("Talk")) // Option C removed
    }

    @Test
    fun `POST pack options removes all options when empty lists submitted`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionA = UUID.randomUUID()
        val optionB = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionA, eventId, name = "Logo")
                insertMockedSponsoringOption(optionB, eventId, name = "Booth")

                // Attach options initially
                insertMockedPackOptions(packId, optionA, required = true)
                insertMockedPackOptions(packId, optionB, required = false)
            }
        }

        // Sync to empty configuration
        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = emptyList(),
                        optional = emptyList(),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)

        // Verify pack has no options
        val verifyResponse = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        val body = json.decodeFromString<List<SponsoringPack>>(verifyResponse.bodyAsText())
        assertEquals(1, body.size)
        assertTrue(body[0].requiredOptions.isEmpty())
        assertTrue(body[0].optionalOptions.isEmpty())
    }
}
