package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.users.factories.insertMockedEventPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UsersRouteTest {
    @Test
    fun `GET returns empty list when no users exist`() = testApplication {
        application {
            moduleMocked()
        }

        val eventId = UUID.randomUUID()
        val response = client.get("/events/$eventId/users")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns only users who can edit`() = testApplication {
        val eventId = UUID.randomUUID()
        val editEmail = "edit@example.com"
        val noEditEmail = "noedit@example.com"

        application {
            moduleMocked()
            val event = insertMockedEvent(eventId)
            insertMockedEventPermission(
                event = event,
                user = insertMockedUser(email = editEmail),
                canEdit = true,
            )
            insertMockedEventPermission(
                event = event,
                user = insertMockedUser(email = noEditEmail),
                canEdit = false,
            )
        }

        val response = client.get("/events/$eventId/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains(editEmail) }
        assertFalse { body.contains(noEditEmail) }
    }

    @Test
    fun `GET returns only users for correct event`() = testApplication {
        val eventIdA = UUID.randomUUID()
        val eventIdB = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEventPermission(
                event = insertMockedEvent(eventIdA),
                user = insertMockedUser(email = "a@example.com"),
                canEdit = true,
            )
            insertMockedEventPermission(
                event = insertMockedEvent(eventIdB),
                user = insertMockedUser(email = "b@example.com"),
                canEdit = true,
            )
        }

        val response = client.get("/events/$eventIdA/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains("a@example.com") }
        assertTrue { !body.contains("b@example.com") }
    }
}
