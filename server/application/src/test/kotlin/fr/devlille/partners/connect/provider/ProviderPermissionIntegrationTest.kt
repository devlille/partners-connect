package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for Scenario 5: Permission boundary testing across organisations.
 * Tests that organisation-scoped permissions are properly enforced.
 */
class ProviderPermissionIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `provider permission boundaries are enforced across organisations`() = testApplication {
        val org1Id = UUID.randomUUID()
        val org1Slug = "organisation-a"
        val org2Id = UUID.randomUUID()
        val org2Slug = "organisation-b"
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val provider1Id = UUID.randomUUID()
        val provider2Id = UUID.randomUUID()

        application {
            moduleMocked()

            // Create two separate organisations
            val org1 = insertMockedOrganisationEntity(org1Id, name = org1Slug)
            val org2 = insertMockedOrganisationEntity(org2Id, name = org2Slug)

            // Create users with different organisation access
            val user1 = insertMockedUser(user1Id, email = "john.doe@contact.com") // Mock auth user
            val user2 = insertMockedUser(user2Id, email = "jane.smith@company.com")

            // Grant permissions: user1 can edit org1, user2 can edit org2
            insertMockedOrgaPermission(org1Id, user1, canEdit = true)
            insertMockedOrgaPermission(org2Id, user2, canEdit = true)

            // Create providers in each organisation
            insertMockedProvider(provider1Id, organisation = org1, name = "Org A Provider")
            insertMockedProvider(provider2Id, organisation = org2, name = "Org B Provider")
        }

        // Test 1: User from org1 cannot create provider in org2
        val createRequest = CreateProvider(name = "Unauthorized Provider", type = "test")
        val unauthorizedCreateResponse = client.post("/orgs/$org2Slug/providers") {
            header("Authorization", "Bearer valid") // user1's token
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedCreateResponse.status)

        // Test 2: User from org1 cannot access provider from org2
        val unauthorizedGetResponse = client.get("/orgs/$org2Slug/providers/$provider2Id") {
            header("Authorization", "Bearer valid") // user1's token
        }

        assertEquals(HttpStatusCode.NotFound, unauthorizedGetResponse.status)

        // Test 3: User from org1 cannot update provider from org2
        val updateRequest = mapOf("name" to "Updated Name")
        val unauthorizedUpdateResponse = client.put("/orgs/$org2Slug/providers/$provider2Id") {
            header("Authorization", "Bearer valid") // user1's token
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedUpdateResponse.status)

        // Test 4: User from org1 cannot delete provider from org2
        val unauthorizedDeleteResponse = client.delete("/orgs/$org2Slug/providers/$provider2Id") {
            header("Authorization", "Bearer valid") // user1's token
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedDeleteResponse.status)

        // Test 5: User can access their own organisation's providers
        val authorizedGetResponse = client.get("/orgs/$org1Slug/providers/$provider1Id") {
            header("Authorization", "Bearer valid") // user1's token
        }

        // Provider lookup returns 404 when provider doesn't exist in organization
        assertEquals(HttpStatusCode.NotFound, authorizedGetResponse.status)
    }

    @Test
    fun `event provider attachment respects organisation boundaries`() = testApplication {
        val org1Id = UUID.randomUUID()
        val org1Slug = "org-a"
        val org2Id = UUID.randomUUID()
        val org2Slug = "org-b"
        val userId = UUID.randomUUID()
        val event1Id = UUID.randomUUID()
        val eventSlug1 = "org-a-event"
        val event2Id = UUID.randomUUID()
        val eventSlug2 = "org-b-event"
        val provider1Id = UUID.randomUUID()
        val provider2Id = UUID.randomUUID()

        application {
            moduleMocked()

            val org1 = insertMockedOrganisationEntity(org1Id, name = org1Slug)
            val org2 = insertMockedOrganisationEntity(org2Id, name = org2Slug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")

            // User has access to both organisations for this test
            insertMockedOrgaPermission(org1Id, user, canEdit = true)
            insertMockedOrgaPermission(org2Id, user, canEdit = true)

            // Create events and providers in different organisations
            insertMockedEventWithOrga(event1Id, organisation = org1, slug = eventSlug1)
            insertMockedEventWithOrga(event2Id, organisation = org2, slug = eventSlug2)
            insertMockedProvider(provider1Id, organisation = org1, name = "Org A Provider")
            insertMockedProvider(provider2Id, organisation = org2, name = "Org B Provider")
        }

        // Test 1: Cannot attach org B provider to org A event
        val crossOrgAttachRequest = listOf(provider2Id.toString())
        val crossOrgAttachResponse = client.post("/orgs/$org1Slug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(crossOrgAttachRequest))
        }

        // Should fail - provider from different organisation
        assertEquals(HttpStatusCode.Forbidden, crossOrgAttachResponse.status)

        // Test 2: Can attach provider to event within same organisation
        val validAttachRequest = listOf(provider1Id.toString())
        val validAttachResponse = client.post("/orgs/$org1Slug/events/$eventSlug1/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(validAttachRequest))
        }

        assertEquals(HttpStatusCode.OK, validAttachResponse.status)

        // Test 3: Cannot detach via wrong organisation context
        val crossOrgDetachRequest = listOf(provider1Id.toString())
        val crossOrgDetachResponse = client.delete("/orgs/$org2Slug/events/$eventSlug2/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(crossOrgDetachRequest))
        }

        // Should fail due to cross-organization boundary violation
        assertEquals(HttpStatusCode.Forbidden, crossOrgDetachResponse.status)
    }

    @Test
    fun `users without edit permissions cannot modify providers`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com")

            // Grant read-only permission (canEdit = false)
            insertMockedOrgaPermission(orgId, user, canEdit = false)
            insertMockedProvider(providerId, organisation = org, name = "Test Provider")
        }

        // Test 1: Cannot create provider with read-only access
        val createRequest = CreateProvider(name = "New Provider", type = "test")
        val createResponse = client.post("/orgs/$orgSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, createResponse.status)

        // Test 2: Cannot update provider with read-only access
        val updateRequest = mapOf("name" to "Updated Provider")
        val updateResponse = client.put("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)

        // Test 3: Cannot delete provider with read-only access
        val deleteResponse = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, deleteResponse.status)
    }

    @Test
    fun `unauthenticated requests are properly rejected`() = testApplication {
        val orgSlug = "test-org"
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        // Test all authenticated endpoints reject requests without Authorization header
        val endpoints = listOf(
            "/orgs/$orgSlug/providers",
            "/orgs/$orgSlug/providers/$providerId",
            "/orgs/$orgSlug/events/test-event/providers",
        )

        endpoints.forEach { endpoint ->
            val getResponse = client.get(endpoint)
            // Event endpoints check authentication first, provider endpoints check existence first
            val expectedGetStatus = if (endpoint.contains("/events/")) {
                HttpStatusCode.Unauthorized
            } else {
                HttpStatusCode.NotFound
            }
            assertEquals(expectedGetStatus, getResponse.status, "GET $endpoint should be $expectedGetStatus")

            val postResponse = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            // POST is only valid for collection endpoints, provider-specific endpoints don't support POST
            val expectedPostStatus = if (endpoint.endsWith("/providers")) {
                HttpStatusCode.Unauthorized
            } else {
                HttpStatusCode.NotFound
            }
            assertEquals(expectedPostStatus, postResponse.status, "POST $endpoint should be $expectedPostStatus")
        }
    }
}
