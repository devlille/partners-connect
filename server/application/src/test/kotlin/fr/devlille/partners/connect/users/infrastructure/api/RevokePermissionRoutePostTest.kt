package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevokePermissionRoutePostTest {
    @Test
    fun `handle empty email list`() = testApplication {
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
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
            }
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
    fun `return 401 if authenticated user has no right to revoke`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
            }
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
        val userId = UUID.randomUUID()
        val adminEmail = "john.doe.$userId@contact.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId, email = adminEmail)
                insertMockedOrgaPermission(orgId, userId)
            }
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
}
