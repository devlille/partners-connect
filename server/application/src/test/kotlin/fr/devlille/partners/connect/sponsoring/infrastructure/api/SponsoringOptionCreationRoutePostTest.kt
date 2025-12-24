package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSelectableValue
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedNumber
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedQuantitative
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedSelectable
import fr.devlille.partners.connect.sponsoring.domain.NumberDescriptor
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

/**
 * Contract tests for POST /orgs/{orgSlug}/events/{eventSlug}/options endpoint.
 * Tests API schema validation for enhanced sponsoring options with four types.
 *
 * IMPORTANT: These tests focus on request/response schema validation only,
 * not business logic. They must fail initially until implementation exists.
 */
class SponsoringOptionCreationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates text option - schema validation`() = testApplication {
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

        val request = CreateText(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Social Media Mention",
                    description = "Company mention on social media",
                ),
            ),
            price = 500,
        )

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates typed_quantitative option - schema validation`() = testApplication {
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

        val request = CreateTypedQuantitative(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Job Offers",
                    description = "Post job offers on job board",
                ),
            ),
            price = 100,
            typeDescriptor = QuantitativeDescriptor.JOB_OFFER,
        )

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates typed_number option - schema validation`() = testApplication {
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

        val request = CreateTypedNumber(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Conference Tickets",
                    description = "Free tickets for team",
                ),
            ),
            price = null,
            typeDescriptor = NumberDescriptor.NB_TICKET,
            fixedQuantity = 5,
        )

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST creates typed_selectable option - schema validation`() = testApplication {
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

        val request = CreateTypedSelectable(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Exhibition Booth",
                    description = "Physical booth space",
                ),
            ),
            typeDescriptor = SelectableDescriptor.BOOTH,
            selectableValues = listOf(
                CreateSelectableValue("3x3m", 100000),
                CreateSelectableValue("3x6m", 150000),
                CreateSelectableValue("6x6m", 200000),
            ),
        )

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST validates required fields - schema validation`() = testApplication {
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

        // Missing type_descriptor for typed_quantitative
        val invalidRequestJson = """
            {
                "translations": [
                    {"language": "en", "name": "Test Option"}
                ],
                "type": "typed_quantitative"
            }
        """.trimIndent()

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(invalidRequestJson)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST validates selectable_values requirement - schema validation`() = testApplication {
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

        // Empty selectable_values for typed_selectable
        val invalidRequestJson = """
            {
                "translations": [
                    {"language": "en", "name": "Test Booth"}
                ],
                "type": "typed_selectable",
                "type_descriptor": "booth",
                "selectable_values": []
            }
        """.trimIndent()

        val response = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(invalidRequestJson)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
