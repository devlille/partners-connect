package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.factories.createOrganisation
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertNotNull

class CreateOrganisationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a organisation`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
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
    fun `POST creates organisation with only name provided`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
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
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
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
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
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
    fun `POST fails if organization already exists`() = testApplication {
        val userId = UUID.randomUUID()
        val organisation = createOrganisation()
        val organisationSlug = organisation.name.slugify()

        application {
            moduleSharedDb(userId)
            transaction {
                val org = insertMockedOrganisationEntity(name = organisation.name)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = org.id.value, userId = userId)
            }
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
    fun `POST fails if representative user does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val organisation = createOrganisation(representativeUserEmail = "nonexistent@example.com")

        application {
            moduleSharedDb(userId)
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
