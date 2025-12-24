package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSelectableValue
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedQuantitative
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedSelectable
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
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

class SponsoringOptionUpdateRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates text option - schema validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId, eventId)
            }
        }

        val request = CreateText(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Social Media",
                    description = "Updated description",
                ),
            ),
            price = 600,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT updates typed_quantitative option - schema validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId, eventId)
            }
        }

        val request = CreateTypedQuantitative(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Job Offers",
                    description = "Updated job board posting",
                ),
            ),
            price = 150,
            typeDescriptor = QuantitativeDescriptor.JOB_OFFER,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT updates typed_selectable option - schema validation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId, eventId)
            }
        }

        val request = CreateTypedSelectable(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Exhibition Booth",
                    description = "Updated booth space",
                ),
            ),
            typeDescriptor = SelectableDescriptor.BOOTH,
            selectableValues = listOf(
                CreateSelectableValue("2x2m", 80000),
                CreateSelectableValue("4x4m", 120000),
                CreateSelectableValue("6x8m", 250000),
            ),
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns error when eventId is invalid UUID`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/not-a-uuid/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns error when optionId is invalid UUID`() = testApplication {
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

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/not-a-uuid") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Request parameter id couldn't be parsed/converted to UUID"))
    }

    @Test
    fun `PUT returns 400 when payload has empty translations`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringOption(optionId, eventId)
            }
        }

        val updateRequest = CreateText(
            translations = emptyList(),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 401 when no authorization header`() = testApplication {
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

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user lacks organization permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer invalid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 when event does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 when option does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }
}
