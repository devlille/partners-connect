package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.domain.TextSelection
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnershipSuggestionRoutePostTest {
    @Test
    fun `PUT suggests a new pack with optional options`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(
                        packId = packId.toString(),
                        language = "en",
                        optionSelections = listOf(TextSelection(optionId = optionId.toString())),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        val suggestionPack = transaction { partnership?.suggestionPack }
        assertEquals(packId, suggestionPack?.id?.value)
        assertNull(partnership?.suggestionApprovedAt)
        assertNull(partnership?.suggestionDeclinedAt)
    }

    @Test
    fun `PUT fails if partnership does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(
                        packId = UUID.randomUUID().toString(),
                        language = "en",
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if pack does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(
                        packId = UUID.randomUUID().toString(),
                        language = "en",
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if option is not optional in pack`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(
                        packId = packId.toString(),
                        language = "en",
                        optionSelections = listOf(TextSelection(optionId = optionId.toString())),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `PUT fails if option translation missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId, language = "fr")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedSponsoringOption(optionId, eventId)
                insertMockedPackOptions(packId, optionId, required = false)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(
                        packId = packId.toString(),
                        language = "en",
                        optionSelections = listOf(TextSelection(optionId = optionId.toString())),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("doesn't have a translation"))
    }
}
