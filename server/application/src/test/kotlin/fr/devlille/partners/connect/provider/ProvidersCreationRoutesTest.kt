package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.internal.moduleSharedDb
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for Scenario 1: Organisation-scoped provider creation workflow.
 * Tests the complete end-to-end workflow from provider creation to listing.
 */
class ProvidersCreationRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    @Suppress("LongMethod") // Integration test requires comprehensive workflow validation
    fun `organisation-scoped provider creation workflow works end-to-end`() = testApplication {
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId1)
                insertMockedOrganisationEntity(orgId2)
                insertMockedOrgaPermission(orgId1, userId = userId)
                insertMockedOrgaPermission(orgId2, userId = userId)
            }
        }

        val createProviderRequest = CreateProvider(
            name = "Tech Solutions Provider",
            type = "Technology Consulting",
            website = "https://techsolutions.example.com",
            phone = "+33123456789",
            email = "contact@techsolutions.example.com",
        )

        // Step 1: Create provider
        val createResponse = client.post("/orgs/$orgId1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createProviderRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdProvider = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        assertNotNull(createdProvider["id"])
        assertEquals("Tech Solutions Provider", createdProvider["name"]?.jsonPrimitive?.content)
        assertEquals(orgId1.toString(), createdProvider["org_slug"]?.jsonPrimitive?.content)

        val providerId = createdProvider["id"]?.jsonPrimitive?.content!!

        // Step 2: Verify provider appears in organisation provider listing
        val listResponse = client.get("/providers?org_slug=$orgId1") {
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

        val updateResponse = client.put("/orgs/$orgId1/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedProvider = Json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject
        assertEquals("Advanced Tech Solutions", updatedProvider["name"]?.jsonPrimitive?.content)
        assertEquals("+33987654321", updatedProvider["phone"]?.jsonPrimitive?.content)

        // Step 4: Verify organisation scoping - try accessing from different organisation
        val crossOrgAccessResponse = client.get("/orgs/$orgId2/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        // Should not find provider from different organisation
        assertEquals(HttpStatusCode.NotFound, crossOrgAccessResponse.status)
    }
}
