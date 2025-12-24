package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.factories.createOrganisation
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EditOrganisationRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates an existing organisation`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val updatedOrg = createOrganisation(name = "Updated Name")
        val response = client.put("/orgs/$orgId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.decodeFromString<Organisation>(response.bodyAsText())
        assertEquals("Updated Name", responseBody.name)
    }

    @Test
    fun `PUT updates organisation with only name provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val minimalUpdatedOrg = createOrganisation(name = "Updated Minimal Name")
        val response = client.put("/orgs/$orgId") {
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
    fun `PUT returns 400 when orgSlug parameter is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
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
    fun `PUT returns 400 when name is empty`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val orgWithEmptyName = createOrganisation(name = "")
        val response = client.put("/orgs/$orgId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), orgWithEmptyName))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("Request parameter 'name' is invalid: must not be empty", message)
    }

    @Test
    fun `PUT returns 401 when user is not authenticated`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.put("/orgs/$orgId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation()))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user has no permission to edit organisation`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
            }
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/$orgId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer invalid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 when representative user does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val updatedOrg = createOrganisation(representativeUserEmail = "nonexistent@example.com")
        val response = client.put("/orgs/$orgId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
        assertEquals("User with email nonexistent@example.com not found", message)
    }

    @Test
    fun `PUT returns 404 when organisation does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        val updatedOrg = createOrganisation()
        val response = client.put("/orgs/non-existent-org") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), updatedOrg))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
