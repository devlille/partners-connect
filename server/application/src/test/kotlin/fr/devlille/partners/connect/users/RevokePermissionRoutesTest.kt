package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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

class RevokePermissionRoutesTest {
    @Test
    fun `revoke users successfully when authenticated user has permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val targetUserId = UUID.randomUUID()
        val email = "bob.${UUID.randomUUID()}@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId, canEdit = true)
                insertMockedUser(targetUserId, email = email)
            }
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(userEmails = listOf(email)),
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
                        (OrganisationPermissionsTable.userId eq targetUserId)
                }
                .firstOrNull()
            assertEquals(null, permission) // Permission should be deleted
        }
    }

    @Test
    fun `return partial success with non-existent users`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RevokePermissionRequest.serializer(),
                    RevokePermissionRequest(
                        userEmails = listOf(
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
}
