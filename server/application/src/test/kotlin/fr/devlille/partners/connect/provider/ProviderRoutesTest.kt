package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.factories.createMockedProviderInput
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProviderRoutesTest {
    @Test
    fun `GET providers returns empty list when no providers exist`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/providers")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET providers returns all providers`() = testApplication {
        application {
            moduleMocked()
            insertMockedProvider(name = "Provider 1", type = "Tech")

            insertMockedProvider(name = "Provider 2", type = "Catering")
        }

        val response = client.get("/providers")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        val providers = Json.parseToJsonElement(responseText).jsonArray

        assertEquals(2, providers.size)
        assertTrue(responseText.contains("Provider 1"))
        assertTrue(responseText.contains("Provider 2"))
    }

    @Test
    fun `POST providers creates provider successfully when user is organizer`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedEvent(id = eventId, orgId = orgId, slug = "test-event")
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val providerInput = createMockedProviderInput(
            name = "New Provider",
            type = "Technology",
            website = "https://newprovider.com",
            phone = "+33987654321",
            email = "contact@newprovider.com",
        )

        val response = client.post("/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateProvider.serializer(), providerInput))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseObject = Json.parseToJsonElement(responseText).jsonObject

        assertNotNull(responseObject["id"])
        // Verify it's a valid UUID format
        UUID.fromString(responseObject["id"]?.toString()?.removeSurrounding("\""))
    }

    @Test
    fun `POST providers fails with 401 when not authenticated`() = testApplication {
        application { moduleMocked() }

        val providerInput = createMockedProviderInput()

        val response = client.post("/providers") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateProvider.serializer(), providerInput))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST providers fails with 403 when user is not organizer`() = testApplication {
        application {
            moduleMocked()
            insertMockedUser() // User without organizer permissions
        }

        val providerInput = createMockedProviderInput()

        val response = client.post("/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateProvider.serializer(), providerInput))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("organizer"))
    }

    @Test
    fun `POST providers fails with 400 for invalid request body`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedEvent(id = eventId, orgId = orgId, slug = "test-event")
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val response = client.post("/providers") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{\"invalid\": \"json\"}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
