package fr.devlille.partners.connect.organisations

import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
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
    fun `POST fails if organization already exists`() = testApplication {
        val organisation = createOrganisation()
        val organisationSlug = organisation.name.slugify()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(name = organisation.name)
            insertMockedEventWithAdminUser(orgId = org.id.value)
        }

        val response = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), organisation))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Organisation with slug $organisationSlug already exists", message)
    }

    @Test
    fun `POST fails if representative user does not exist`() {
        val organisation = createOrganisation(representativeUserEmail = "nonexistent@example.com")

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
            val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
            assertEquals("User with email ${organisation.representativeUserEmail} not found", message)
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
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("User with email nonexistent@example.com not found", message)
    }

    @Test
    fun `POST creates organisation with only name provided`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val minimalOrganisation = createOrganisation(name = "Minimal Org")
        val response = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), minimalOrganisation))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertNotNull(responseBody["slug"], "Response should contain a 'slug' field")
    }

    @Test
    fun `POST returns 400 when name is empty`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val orgWithEmptyName = createOrganisation(name = "")
        val response = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), orgWithEmptyName))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Request parameter 'name' is invalid: must not be empty", message)
    }

    @Test
    fun `POST returns 400 when name is blank`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val orgWithBlankName = createOrganisation(name = "   ")
        val response = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), orgWithBlankName))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Request parameter 'name' is invalid: must not be empty", message)
    }

    @Test
    fun `PUT updates organisation with only name provided`() = testApplication {
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

        val minimalUpdatedOrg = createOrganisation(name = "Updated Minimal Name")
        val response = client.put("/orgs/original-name") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), minimalUpdatedOrg))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.decodeFromString<Organisation>(response.bodyAsText())
        assertEquals("Updated Minimal Name", responseBody.name)
        // All other fields should be null in the response
        assertEquals(null, responseBody.headOffice)
        assertEquals(null, responseBody.iban)
        assertEquals(null, responseBody.representativeUserEmail)
    }

    @Test
    fun `PUT returns 400 when name is empty`() = testApplication {
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

        val orgWithEmptyName = createOrganisation(name = "")
        val response = client.put("/orgs/test-org") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), orgWithEmptyName))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Request parameter 'name' is invalid: must not be empty", message)
    }

    @Test
    fun `GET returns organisation with null fields when minimal data`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        // Create a minimal organisation
        val postResponse = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation(name = "Minimal Test Org")))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        val slug = postResponseBody["slug"]!!

        val getResponse = client.get("/orgs/$slug")

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val organisation = Json.decodeFromString<Organisation>(getResponse.bodyAsText())
        assertEquals("Minimal Test Org", organisation.name)
        assertEquals(null, organisation.headOffice)
        assertEquals(null, organisation.iban)
        assertEquals(null, organisation.bic)
        assertEquals(null, organisation.representativeUserEmail)
    }
}
