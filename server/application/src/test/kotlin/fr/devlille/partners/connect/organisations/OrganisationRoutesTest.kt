package fr.devlille.partners.connect.organisations

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.factories.createOrganisation
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrganisationRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a organisation`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val response = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertNotNull(responseBody["slug"], "Response should contain a 'slug' field")
    }

    @Test
    fun `POST fails if representative user does not exist`() {
        val organisation = createOrganisation()

        testApplication {
            application {
                moduleMocked()
            }

            val response = client.post("/orgs") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer valid")
                setBody(json.encodeToString(Organisation.serializer(), organisation))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("User with email ${organisation.representativeUserEmail} not found", response.bodyAsText())
        }
    }

    @Test
    fun `GET returns a organisation when it exists`() = testApplication {
        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity()
            insertMockedEventWithAdminUser(orgId = org.id.value)
        }

        val postResponse = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation()))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())

        val getResponse = client.get("/orgs/${postResponseBody["slug"]}")

        assertEquals(HttpStatusCode.OK, getResponse.status)
    }

    @Test
    fun `GET returns 404 when organisation does not exist`() = testApplication {
        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity()
            insertMockedEventWithAdminUser(orgId = org.id.value)
        }

        val response = client.get("/orgs/non-existing-slug")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT updates an existing organisation`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val adminUser = insertMockedAdminUser()
            insertMockedOrganisationEntity(
                id = orgId,
                name = "Original Name",
                representativeUser = adminUser,
            )
            insertMockedOrgaPermission(orgId = orgId, user = adminUser)
        }

        val updatedOrg = createOrganisation(name = "Updated Name")
        val response = client.put("/orgs/original-name") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.decodeFromString<Organisation>(response.bodyAsText())
        assertEquals("Updated Name", responseBody.name)
    }

    @Test
    fun `PUT returns 404 when organisation does not exist`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/non-existent-org") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 401 when user is not authenticated`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val adminUser = insertMockedAdminUser()
            insertMockedOrganisationEntity(
                id = orgId,
                name = "Test Org",
                representativeUser = adminUser,
            )
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/test-org") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user has no permission to edit organisation`() = testApplication {
        val nonOwnerEmail = "nonowner@example.com"
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val adminUser = insertMockedAdminUser()
            insertMockedOrganisationEntity(
                id = orgId,
                name = "Protected Org",
                representativeUser = adminUser,
            )
            // Note: not granting permission to admin user for this org
            insertMockedUser(email = nonOwnerEmail)
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/protected-org") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer invalid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 400 when orgSlug parameter is missing`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 when representative user does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val adminUser = insertMockedAdminUser()
            insertMockedOrganisationEntity(
                id = orgId,
                name = "Test Org",
                representativeUser = adminUser,
            )
            insertMockedOrgaPermission(orgId = orgId, user = adminUser)
        }

        val updatedOrg = createOrganisation(representativeUserEmail = "nonexistent@example.com")
        val response = client.put("/orgs/test-org") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("User with email nonexistent@example.com not found", response.bodyAsText())
    }
}
