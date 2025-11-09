package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.UpdateProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for Scenario 1: Organisation-scoped provider creation workflow.
 * Tests the complete end-to-end workflow from provider creation to listing.
 */
class ProviderCreationIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    fun `organisation-scoped provider creation workflow works end-to-end`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()

        // Set up test data for cross-org verification
        val otherOrgId = UUID.randomUUID()
        val otherOrgSlug = "other-org"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match mock auth
            insertMockedOrgaPermission(orgId, user, canEdit = true)

            // Setup for cross-org verification test
            insertMockedOrganisationEntity(otherOrgId, name = otherOrgSlug)
            insertMockedOrgaPermission(otherOrgId, user, canEdit = true) // Same user, different org
        }

        val createProviderRequest = CreateProvider(
            name = "Tech Solutions Provider",
            type = "Technology Consulting",
            website = "https://techsolutions.example.com",
            phone = "+33123456789",
            email = "contact@techsolutions.example.com",
        )

        // Step 1: Create provider
        val createResponse = client.post("/orgs/$orgSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createProviderRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdProvider = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        assertNotNull(createdProvider["id"])
        assertEquals("Tech Solutions Provider", createdProvider["name"]?.jsonPrimitive?.content)
        assertEquals(orgSlug, createdProvider["org_slug"]?.jsonPrimitive?.content)

        val providerId = createdProvider["id"]?.jsonPrimitive?.content!!

        // Step 2: Verify provider appears in organisation provider listing
        val listResponse = client.get("/providers?org_slug=$orgSlug") {
            // No authorization needed for public listing
        }

        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listResult = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val items = listResult["items"]?.jsonArray
        assertTrue(items!!.isNotEmpty())
        assertTrue(items.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Tech Solutions Provider" })

        // Step 3: Update provider to test complete CRUD workflow
        val updateRequest = UpdateProvider(
            name = "Advanced Tech Solutions",
            phone = "+33987654321",
        )

        val updateResponse = client.put("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedProvider = Json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject
        assertEquals("Advanced Tech Solutions", updatedProvider["name"]?.jsonPrimitive?.content)
        assertEquals("+33987654321", updatedProvider["phone"]?.jsonPrimitive?.content)

        // Step 4: Verify organisation scoping - try accessing from different organisation
        val crossOrgAccessResponse = client.get("/orgs/$otherOrgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        // Should not find provider from different organisation
        assertEquals(HttpStatusCode.NotFound, crossOrgAccessResponse.status)
    }

    @Test
    fun `provider creation validates organisation membership`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")
            // Note: No organisation permission granted
        }

        val createRequest = CreateProvider(
            name = "Unauthorized Provider",
            type = "test",
        )

        val response = client.post("/orgs/$orgSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createRequest))
        }

        // Should be unauthorized due to lack of organisation permissions
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
