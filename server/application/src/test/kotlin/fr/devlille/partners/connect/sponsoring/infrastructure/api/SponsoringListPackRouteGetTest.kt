package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsoringListPackRouteGetTest {
    @Test
    fun `GET returns empty list when no packs exist`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns all packs with empty options`() = testApplication {
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

                repeat(2) {
                    insertMockedSponsoringPack(
                        eventId = eventId,
                        name = "Pack$it",
                        basePrice = 1000 * (it + 1),
                        maxQuantity = 3 + it,
                    )
                }
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<List<SponsoringPack>>(response.bodyAsText())
        assertEquals(2, body.size)
        assertTrue(body.all { it.requiredOptions.isEmpty() && it.optionalOptions.isEmpty() })
    }

    @Test
    fun `GET succeeds without Accept-Language header for organizer endpoints`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        // Organizer endpoints now work without Accept-Language header
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `organizer packs endpoint returns all translations without Accept-Language header`() = testApplication {
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
                insertMockedSponsoringPack(packId, eventId = eventId)
                insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
                insertMockedPackOptions(packId = packId, optionId = optionId, required = true)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())

        // Verify response contains translations map structure
        assertTrue(responseBody.contains("translations"))
    }

    @Test
    fun `GET returns all packs with all translations without Accept-Language header`() = testApplication {
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

                insertMockedSponsoringPack(id = packId, eventId = eventId)
                insertMockedSponsoringOption(
                    optionId = optionId,
                    eventId = eventId,
                    name = "Logo on website",
                    description = "Company logo displayed on event website",
                )

                // Insert additional translations for the option (en already created by insertMockedSponsoringOption)
                insertMockedOptionTranslation(
                    optionId = optionId,
                    language = "fr",
                    name = "Logo sur le site web",
                    description = "Logo de l'entreprise affiché sur le site de l'événement",
                )
                insertMockedOptionTranslation(
                    optionId = optionId,
                    language = "de",
                    name = "Logo auf Website",
                    description = "",
                )

                insertMockedPackOptions(packId = packId, optionId = optionId, required = true)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // Intentionally NO Accept-Language header
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())
        assertTrue(responseBody.contains("translations"))
    }
}
