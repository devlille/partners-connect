package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import fr.devlille.partners.connect.users.infrastructure.api.RevokePermissionRequest
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevokePermissionRouteTest {
    @Test
    fun `revoke users successfully when authenticated user has permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val targetId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            val user = insertMockedUser(id = targetId, email = "bob@example.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf("bob@example.com")),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("revoked_count"))
        assertTrue(body.contains("not_found_emails"))

        transaction {
            val permission = OrganisationPermissionEntity
                .find {
                    (OrganisationPermissionsTable.organisationId eq orgId) and
                        (OrganisationPermissionsTable.userId eq targetId)
                }
                .firstOrNull()
            assertEquals(null, permission) // Permission should be deleted
        }
    }

    @Test
    fun `return partial success with non-existent users`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            val user = insertMockedUser(id = aliceId, email = "alice@example.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(
                        userEmails = listOf(
                            "alice@example.com",
                            "nonexistent@example.com",
                        ),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("revoked_count"))
        assertTrue(body.contains("nonexistent@example.com"))
    }

    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = emptyList()),
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = emptyList()),
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 404 if authenticated user is not in DB`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf("test@example.com")),
                ),
            )
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `return 401 if authenticated user has no right to revoke`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(eventId, orgId = orgId)
            // Insert admin user without edit permission on the org
            insertMockedAdminUser()
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf("test@example.com")),
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 409 when revoking last editor's own access`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val adminEmail = "john.doe@contact.com"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf(adminEmail)),
                ),
            )
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("last editor"))
    }

    @Test
    fun `handle empty email list`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = emptyList()),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("revoked_count"))
        assertTrue(body.contains("not_found_emails"))
    }

    @Test
    fun `idempotent - revoking already revoked user`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedUser(id = userId, email = "bob@example.com")
            // Note: NOT granting permission to bob
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf("bob@example.com")),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("revoked_count"))
        assertTrue(body.contains("bob@example.com")) // In not_found_emails
    }
}
