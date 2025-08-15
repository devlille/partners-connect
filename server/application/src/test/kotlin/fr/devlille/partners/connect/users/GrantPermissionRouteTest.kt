package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedUser
import fr.devlille.partners.connect.users.infrastructure.api.GrantPermissionRequest
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
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

class GrantPermissionRouteTest {
    @Test
    fun `grant users when authenticated user has permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedUser(id = targetId, email = "bob@example.com")
        }

        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf("bob@example.com"))))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val perms = OrganisationPermissionEntity.all().toList()
            assertEquals(2, perms.size)
            assertEquals(true, perms.find { it.user.id.value == targetId }?.canEdit)
        }
    }

    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
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
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
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

        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `return 401 if authenticated user has no right to grant`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val email = "noedit@mail.com"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(eventId, orgId = orgId)
            insertMockedAdminUser()
            insertMockedUser(email = email)
        }
        val response = client.post("/orgs/$orgId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf(email))))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
