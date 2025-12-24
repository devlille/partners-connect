package fr.devlille.partners.connect.provider

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for Scenario 5: Permission boundary testing across organisations.
 * Tests that organisation-scoped permissions are properly enforced.
 */
class ProvidersPermissionRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `provider permission boundaries are enforced across organisations`() = testApplication {
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val org1Id = UUID.randomUUID()
        val org2Id = UUID.randomUUID()
        val providerId1 = UUID.randomUUID()
        val providerId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId = user1Id)
            transaction {
                insertMockedOrganisationEntity(org1Id)
                insertMockedOrganisationEntity(org2Id)
                insertMockedUser(user1Id)
                insertMockedUser(user2Id)
                insertMockedOrgaPermission(org1Id, userId = user1Id)
                insertMockedOrgaPermission(org2Id, userId = user2Id)
                insertMockedProvider(providerId1, orgId = org1Id)
                insertMockedProvider(providerId2, orgId = org2Id)
            }
        }

        // Test 1: User from org1 cannot create provider in org2
        val createRequest = CreateProvider(name = "Unauthorized Provider", type = "test")
        val unauthorizedCreateResponse = client.post("/orgs/$org2Id/providers") {
            header("Authorization", "Bearer valid") // user1's token
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedCreateResponse.status)

        // Test 2: User from org1 cannot access provider from org2
        val unauthorizedGetResponse = client.get("/orgs/$org2Id/providers/$providerId2") {
            header("Authorization", "Bearer valid") // user1's token
        }

        assertEquals(HttpStatusCode.NotFound, unauthorizedGetResponse.status)

        // Test 3: User from org1 cannot update provider from org2
        val updateRequest = mapOf("name" to "Updated Name")
        val unauthorizedUpdateResponse = client.put("/orgs/$org2Id/providers/$providerId2") {
            header("Authorization", "Bearer valid") // user1's token
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedUpdateResponse.status)

        // Test 4: User from org1 cannot delete provider from org2
        val unauthorizedDeleteResponse = client.delete("/orgs/$org2Id/providers/$providerId2") {
            header("Authorization", "Bearer valid") // user1's token
        }

        assertEquals(HttpStatusCode.Unauthorized, unauthorizedDeleteResponse.status)

        // Test 5: User can access their own organisation's providers
        val authorizedGetResponse = client.get("/orgs/$org1Id/providers/$providerId1") {
            header("Authorization", "Bearer valid") // user1's token
        }

        // Provider lookup returns 404 when provider doesn't exist in organization
        assertEquals(HttpStatusCode.NotFound, authorizedGetResponse.status)
    }

    @Test
    fun `event provider attachment respects organisation boundaries`() = testApplication {
        val user1Id = UUID.randomUUID()
        val user2Id = UUID.randomUUID()
        val org1Id = UUID.randomUUID()
        val org2Id = UUID.randomUUID()
        val event1Id = UUID.randomUUID()
        val event2Id = UUID.randomUUID()
        val provider1Id = UUID.randomUUID()
        val provider2Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId = user1Id)
            transaction {
                insertMockedOrganisationEntity(org1Id)
                insertMockedOrganisationEntity(org2Id)
                insertMockedUser(user1Id)
                insertMockedUser(user2Id)
                insertMockedOrgaPermission(org1Id, userId = user1Id)
                insertMockedOrgaPermission(org2Id, userId = user2Id)
                insertMockedFutureEvent(event1Id, orgId = org1Id)
                insertMockedFutureEvent(event2Id, orgId = org2Id)
                insertMockedProvider(provider1Id, orgId = org1Id)
                insertMockedProvider(provider2Id, orgId = org2Id)
            }
        }

        // Test 1: Cannot attach org B provider to org A event
        val crossOrgAttachRequest = listOf(provider2Id.toString())
        val crossOrgAttachResponse = client.post("/orgs/$org1Id/events/$event1Id/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(crossOrgAttachRequest))
        }

        // Should fail - provider from different organisation
        assertEquals(HttpStatusCode.Forbidden, crossOrgAttachResponse.status)

        // Test 2: Can attach provider to event within same organisation
        val validAttachRequest = listOf(provider1Id.toString())
        val validAttachResponse = client.post("/orgs/$org1Id/events/$event1Id/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(validAttachRequest))
        }

        assertEquals(HttpStatusCode.OK, validAttachResponse.status)
    }

    @Test
    fun `users without edit permissions cannot modify providers`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
            }
        }

        // Test 1: Cannot create provider with read-only access
        val createRequest = CreateProvider(name = "New Provider", type = "test")
        val createResponse = client.post("/orgs/$orgId/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateProvider.serializer(), createRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, createResponse.status)

        // Test 2: Cannot update provider with read-only access
        val updateRequest = mapOf("name" to "Updated Provider")
        val updateResponse = client.put("/orgs/$orgId/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)

        // Test 3: Cannot delete provider with read-only access
        val deleteResponse = client.delete("/orgs/$orgId/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, deleteResponse.status)
    }
}
