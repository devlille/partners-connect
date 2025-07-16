package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.auth.infrastructure.bindings.authModule
import fr.devlille.partners.connect.events.infrastructure.bindings.eventModule
import fr.devlille.partners.connect.internal.mockNetworkingEngineModule
import fr.devlille.partners.connect.internal.mockedAdminUser
import fr.devlille.partners.connect.module
import fr.devlille.partners.connect.users.infrastructure.api.GrantPermissionRequest
import fr.devlille.partners.connect.users.infrastructure.bindings.userModule
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
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
    fun `return 401 if no Authorization header`() = testApplication {
        application {
            module()
        }
        val response = client.post("/events/${UUID.randomUUID()}/users/grant") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        application {
            module(
                modules = listOf(mockNetworkingEngineModule, authModule, eventModule, userModule),
            )
        }

        val response = client.post("/events/${UUID.randomUUID()}/users/grant") {
            header("Authorization", "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 404 if authenticated user is not in DB`() = testApplication {
        application {
            module(
                modules = listOf(mockNetworkingEngineModule, authModule, eventModule, userModule),
            )
        }

        val response = client.post("/events/${UUID.randomUUID()}/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = emptyList())))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `return 401 if authenticated user has no right to grant`() = testApplication {
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            module(
                databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                modules = listOf(mockNetworkingEngineModule, authModule, eventModule, userModule),
            )
            transaction {
                UserEntity.new(userId) {
                    email = mockedAdminUser.email
                    name = mockedAdminUser.name
                }
                // No permission given
            }
        }
        val response = client.post("/events/$eventId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf(UUID.randomUUID().toString()))))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `grant users when authenticated user has permission`() = testApplication {
        val eventId = UUID.randomUUID()
        val granterId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        application {
            module(
                databaseUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                modules = listOf(mockNetworkingEngineModule, authModule, eventModule, userModule),
            )
            transaction {
                val granter = UserEntity.new(granterId) {
                    email = mockedAdminUser.email
                    name = mockedAdminUser.name
                }
                UserEntity.new(targetId) {
                    email = "bob@example.com"
                    name = "Bob"
                }
                EventPermissionEntity.new {
                    this.eventId = eventId
                    this.user = granter
                    this.canEdit = true
                }
            }
        }

        val response = client.post("/events/$eventId/users/grant") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(GrantPermissionRequest(userEmails = listOf("bob@example.com"))))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        transaction {
            val perms = EventPermissionEntity.all().toList()
            assertEquals(2, perms.size)
            assertEquals(true, perms.find { it.user.id.value == targetId }?.canEdit)
        }
    }
}
