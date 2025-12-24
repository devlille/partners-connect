package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import io.ktor.client.request.header
import io.ktor.client.request.post
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

class GrantPermissionRoutePostTest {
    @Test
    fun `grant users when authenticated user has permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
                insertMockedUser(email = "bob@example.com")
            }
        }

        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf("bob@example.com"))))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val perms = OrganisationPermissionEntity
                .find { OrganisationPermissionsTable.organisationId eq orgId }
                .toList()
            assertEquals(2, perms.size)
            assertEquals(true, perms.find { it.user.id.value == userId }?.canEdit)
        }
    }

    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
            }
        }

        val response = client.post("/orgs/$orgId/users/grant") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
            }
        }

        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if authenticated user has no right to grant`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val email = "noedit@mail.com"
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
            }
        }
        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf(email))))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
