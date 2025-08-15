package fr.devlille.partners.connect.legalentity

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.legaentity.domain.LegalEntity
import fr.devlille.partners.connect.legalentity.factories.createLegalEntity
import fr.devlille.partners.connect.legalentity.factories.insertLegalEntity
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
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

class LegalEntityRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a legal entity`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(UUID.randomUUID())
        }

        val response = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), createLegalEntity()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertNotNull(responseBody["id"], "Response should contain an 'id' field")
    }

    @Test
    fun `POST fails if representative user does not exist`() {
        val legalEntity = createLegalEntity()

        testApplication {
            application {
                moduleMocked()
            }

            val response = client.post("/legal-entities") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer valid")
                setBody(json.encodeToString(LegalEntity.serializer(), legalEntity))
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("User with email ${legalEntity.representativeUserEmail} not found", response.bodyAsText())
        }
    }

    @Test
    fun `GET returns a legal entity when it exists`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser()
        }

        val postResponse = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), createLegalEntity()))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        val legalEntityId = UUID.fromString(postResponseBody["id"])

        val getResponse = client.get("/legal-entities/$legalEntityId")

        assertEquals(HttpStatusCode.OK, getResponse.status)
    }

    @Test
    fun `GET returns 404 when legal entity does not exist`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(UUID.randomUUID())
        }

        val response = client.get("/legal-entities/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT updates a legal entity successfully`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(UUID.randomUUID())
        }

        // First create a legal entity
        val postResponse = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), createLegalEntity()))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        val legalEntityId = UUID.fromString(postResponseBody["id"])

        // Now update it
        val updatedEntity = createLegalEntity(name = "Updated DevLille Org", headOffice = "456 rue de la Paix, Lille, France")
        val putResponse = client.put("/legal-entities/$legalEntityId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), updatedEntity))
        }

        assertEquals(HttpStatusCode.OK, putResponse.status)
        val responseEntity = json.decodeFromString(LegalEntity.serializer(), putResponse.bodyAsText())
        assertEquals("Updated DevLille Org", responseEntity.name)
        assertEquals("456 rue de la Paix, Lille, France", responseEntity.headOffice)
    }

    @Test
    fun `PUT returns 404 when legal entity does not exist`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(UUID.randomUUID())
        }

        val updatedEntity = createLegalEntity()
        val response = client.put("/legal-entities/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), updatedEntity))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 401 when user is not authenticated`() = testApplication {
        application {
            moduleMocked()
            insertMockedEventWithAdminUser(UUID.randomUUID())
        }

        // First create a legal entity
        val postResponse = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), createLegalEntity()))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        val legalEntityId = UUID.fromString(postResponseBody["id"])

        // Try to update without authentication
        val updatedEntity = createLegalEntity()
        val response = client.put("/legal-entities/$legalEntityId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(LegalEntity.serializer(), updatedEntity))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user is not the representative user`() = testApplication {
        val entityId = UUID.randomUUID()
        application {
            moduleMocked()
            // Create a legal entity with a different user as representative  
            val differentUser = insertMockedUser(email = "different@user.com", name = "Different User")
            insertLegalEntity(id = entityId, representativeUser = differentUser)
        }

        // The authenticated admin user tries to update a legal entity owned by different@user.com
        val updatedEntity = createLegalEntity()
        val response = client.put("/legal-entities/$entityId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(LegalEntity.serializer(), updatedEntity))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
