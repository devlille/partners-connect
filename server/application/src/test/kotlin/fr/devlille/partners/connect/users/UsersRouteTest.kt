package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
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
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.get("/orgs/$orgId/users")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns only users who can edit`() = testApplication {
        val orgId = UUID.randomUUID()
        val editEmail = "edit@example.com"
        val noEditEmail = "noedit@example.com"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedOrgaPermission(
                orgId = orgId,
                user = insertMockedUser(email = editEmail),
                canEdit = true,
            )
            insertMockedOrgaPermission(
                orgId = orgId,
                user = insertMockedUser(email = noEditEmail),
                canEdit = false,
            )
        }

        val response = client.get("/orgs/$orgId/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains(editEmail) }
        assertFalse { body.contains(noEditEmail) }
    }

    @Test
    fun `GET returns only users for correct org`() = testApplication {
        val orgIdA = UUID.randomUUID()
        val orgIdB = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(id = orgIdA)
            insertMockedOrganisationEntity(id = orgIdB)
            insertMockedOrgaPermission(
                orgId = orgIdA,
                user = insertMockedUser(email = "a@example.com"),
                canEdit = true,
            )
            insertMockedOrgaPermission(
                orgId = orgIdB,
                user = insertMockedUser(email = "b@example.com"),
                canEdit = true,
            )
        }

        val response = client.get("/orgs/$orgIdA/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains("a@example.com") }
        assertTrue { !body.contains("b@example.com") }
    }
}
