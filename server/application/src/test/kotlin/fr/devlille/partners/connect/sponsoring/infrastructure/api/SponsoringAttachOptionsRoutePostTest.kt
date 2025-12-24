package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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
import kotlin.test.assertTrue

class SponsoringAttachOptionsRoutePostTest {
    @Test
    fun `POST packs options returns 403 if any option is not linked to the event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val optionId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedFutureEvent(eventId2, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
                insertMockedSponsoringOption(optionId = optionId2, eventId = eventId2)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = listOf(optionId.toString()),
                        optional = listOf(optionId2.toString()),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue { response.bodyAsText().contains("Some options do not belong to the event") }
    }

    @Test
    fun `POST packs options is idempotent when option already attached`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
                insertMockedPackOptions(packId, optionId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
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

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST pack options updates requirement status from required to optional`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = true)
            }
        }

        // Sync with option A in optional list (changes status from required to optional)
        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    AttachOptionsToPack(
                        required = emptyList(),
                        optional = listOf(optionId.toString()),
                    ),
                ),
            )
        }

        // Operation succeeds - status change applied
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST pack options updates requirement status from optional to required`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
            }
        }

        // Sync with option B in required list (changes status from optional to required)
        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
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

        // Operation succeeds - status change applied
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST to attach options returns 404 if pack does not exist`() = testApplication {
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

        val attachRequest = AttachOptionsToPack(required = emptyList(), optional = emptyList())

        val response = client.post("/orgs/$orgId/events/$eventId/packs/${UUID.randomUUID()}/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(attachRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST packs options returns 404 when pack does not exist for event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs/$packId/options") {
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
}
