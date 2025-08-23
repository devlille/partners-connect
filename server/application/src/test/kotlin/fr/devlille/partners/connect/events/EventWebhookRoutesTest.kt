package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.domain.CreateEventWebhookRequest
import fr.devlille.partners.connect.events.domain.EventWebhook
import fr.devlille.partners.connect.events.domain.WebhookType
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventWebhookRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST webhooks creates webhook and returns 201`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventWebhookRequest(
            url = "https://example.com/webhook",
            type = WebhookType.ALL,
            headerAuth = "Bearer secret-token",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateEventWebhookRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = json.parseToJsonElement(response.body<String>()).jsonObject
        val webhookId = responseBody["id"]?.jsonPrimitive?.content
        assertNotNull(webhookId)
        assertTrue(webhookId.isNotBlank())
    }

    @Test
    fun `POST webhooks with partnership type requires partnershipId`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            val event = insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventWebhookRequest(
            url = "https://example.com/webhook",
            type = WebhookType.PARTNERSHIP,
            partnershipId = partnershipId.toString(),
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateEventWebhookRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `GET webhooks returns list of webhooks`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        // Create a webhook first
        val createRequest = CreateEventWebhookRequest(
            url = "https://example.com/webhook",
            type = WebhookType.ALL,
        )

        client.post("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateEventWebhookRequest.serializer(), createRequest))
        }

        // Get the webhooks
        val response = client.get("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val webhooks = json.decodeFromString<List<EventWebhook>>(response.body<String>())
        assertTrue(webhooks.isNotEmpty())
        assertEquals("https://example.com/webhook", webhooks.first().url)
        assertEquals(WebhookType.ALL, webhooks.first().type)
    }

    @Test
    fun `DELETE webhook removes webhook and returns 204`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        // Create a webhook first
        val createRequest = CreateEventWebhookRequest(
            url = "https://example.com/webhook",
            type = WebhookType.ALL,
        )

        val createResponse = client.post("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateEventWebhookRequest.serializer(), createRequest))
        }

        val createResponseBody = json.parseToJsonElement(createResponse.body<String>()).jsonObject
        val webhookId = createResponseBody["id"]?.jsonPrimitive?.content!!

        // Delete the webhook
        val deleteResponse = client.delete("/orgs/$orgSlug/events/$eventSlug/webhooks/$webhookId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `POST webhooks with invalid URL returns 400`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventWebhookRequest(
            url = "invalid-url",
            type = WebhookType.ALL,
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/webhooks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateEventWebhookRequest.serializer(), request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}