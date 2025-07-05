package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.module
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsersRouteTest {

    @Test
    fun `GET returns empty list when no users exist`() = testApplication {
        application {
            module()
        }

        val eventId = UUID.randomUUID()
        val response = client.get("/events/$eventId/users")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns only users who can edit`() = testApplication {
        val eventId = UUID.randomUUID()

        application {
            module()
            val user1 = transaction {
                UserEntity.new { email = "edit@example.com"; name = "Alice" }
            }
            val user2 = transaction {
                UserEntity.new { email = "noedit@example.com"; name = "Bob" }
            }
            transaction {
                EventPermissionEntity.new {
                    this.eventId = eventId
                    this.user = user1
                    this.canEdit = true
                }
                EventPermissionEntity.new {
                    this.eventId = eventId
                    this.user = user2
                    this.canEdit = false
                }
            }
        }

        val response = client.get("/events/$eventId/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains("edit@example.com") }
        assertFalse { body.contains("noedit@example.com") }
    }

    @Test
    fun `GET returns only users for correct event`() = testApplication {
        val eventA = UUID.randomUUID()
        val eventB = UUID.randomUUID()
        application {
            module()
            val userA = transaction {
                UserEntity.new { email = "a@example.com" }
            }
            val userB = transaction {
                UserEntity.new { email = "b@example.com" }
            }

            transaction {
                EventPermissionEntity.new {
                    this.eventId = eventA
                    this.user = userA
                    this.canEdit = true
                }
                EventPermissionEntity.new {
                    this.eventId = eventB
                    this.user = userB
                    this.canEdit = true
                }
            }
        }

        val response = client.get("/events/$eventA/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains("a@example.com") }
        assertTrue { !body.contains("b@example.com") }
    }
}
