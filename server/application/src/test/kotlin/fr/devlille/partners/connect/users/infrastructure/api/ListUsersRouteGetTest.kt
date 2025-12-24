package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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

class ListUsersRouteGetTest {
    @Test
    fun `GET returns empty list when no users exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
            }
        }

        val response = client.get("/orgs/$orgId/users")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns only users who can edit`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val editEmail = "edit@example.com"
        val noEditEmail = "noedit@example.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(
                    orgId = orgId,
                    userId = insertMockedUser(email = editEmail).id.value,
                    canEdit = true,
                )
                insertMockedOrgaPermission(
                    orgId = orgId,
                    userId = insertMockedUser(email = noEditEmail).id.value,
                    canEdit = false,
                )
            }
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
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgIdA)
                insertMockedOrganisationEntity(id = orgIdB)
                insertMockedOrgaPermission(
                    orgId = orgIdA,
                    userId = insertMockedUser(email = "a@example.com").id.value,
                    canEdit = true,
                )
                insertMockedOrgaPermission(
                    orgId = orgIdB,
                    userId = insertMockedUser(email = "b@example.com").id.value,
                    canEdit = true,
                )
            }
        }

        val response = client.get("/orgs/$orgIdA/users")
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { body.contains("a@example.com") }
        assertTrue { !body.contains("b@example.com") }
    }
}
