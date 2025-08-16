package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUserOrganisationsRouteTest {
    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/orgs")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 404 if authenticated user is not in DB`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `return empty array if organizer has no organizations`() = testApplication {
        val userId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            insertMockedUser(userId, email = email)
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `return organizations array if organizer has organizations`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(
                id = orgId,
                name = "GDG Lille",
                headOffice = "74 Rue des Beaux Arts, 59000 Lille",
                representativeUser = user,
            )
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val orgsArray = json.jsonArray

        assertEquals(1, orgsArray.size)

        val org = orgsArray[0].jsonObject
        assertEquals("GDG Lille", org["name"]?.jsonPrimitive?.content)
        assertEquals("gdg-lille", org["slug"]?.jsonPrimitive?.content)
        assertEquals("74 Rue des Beaux Arts, 59000 Lille", org["head_office"]?.jsonPrimitive?.content)

        val owner = org["owner"]?.jsonObject
        assertTrue(owner != null)
        assertTrue(owner.containsKey("display_name"))
        assertEquals(email, owner["email"]?.jsonPrimitive?.content)
    }

    @Test
    fun `return multiple organizations for organizer with multiple memberships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)

            // First organization
            insertMockedOrganisationEntity(
                id = orgId1,
                name = "GDG Lille",
                headOffice = "74 Rue des Beaux Arts, 59000 Lille",
                representativeUser = user,
            )
            insertMockedOrgaPermission(orgId = orgId1, user = user, canEdit = true)

            // Second organization
            insertMockedOrganisationEntity(
                id = orgId2,
                name = "Developers Lille",
                headOffice = "1 Place de la République, 59000 Lille",
                representativeUser = user,
            )
            insertMockedOrgaPermission(orgId = orgId2, user = user, canEdit = true)
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val orgsArray = json.jsonArray

        assertEquals(2, orgsArray.size)

        val orgSlugs = orgsArray.map { it.jsonObject["slug"]?.jsonPrimitive?.content }
        assertTrue(orgSlugs.contains("gdg-lille"))
        assertTrue(orgSlugs.contains("developers-lille"))
    }

    @Test
    fun `does not return organizations where user has no edit permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(
                id = orgId,
                name = "GDG Lille",
                representativeUser = user,
            )
            // Grant permission but set canEdit = false
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = false)
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }
}
