package fr.devlille.partners.connect.legalentity

import fr.devlille.partners.connect.internal.insertMockedAdminUser
import fr.devlille.partners.connect.internal.mockedAdminUser
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LegalEntityRoutesTest {
    private val testLegalEntityJson = """
        {
            "name": "DevLille Org",
            "head_office": "123 rue de la République, Lille, France",
            "siret": "12345678900019",
            "siren": "123456789",
            "tva": "FR123456789",
            "d_and_b": "123456789",
            "nace": "62.01Z",
            "naf": "62.01Z",
            "duns": "987654321",
            "iban": "FR7630006000011234567890189",
            "bic": "AGRIFRPPXXX",
            "ribUrl": "https://example.com/rib.pdf",
            "representative_user_email": "${mockedAdminUser.email}",
            "representative_role": "President",
            "creation_location": "Lille",
            "created_at": "2025-08-01T00:00:00",
            "published_at": "2025-08-02T00:00:00"
        }
    """.trimIndent()

    @Test
    fun `POST creates a legal entity`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser(UUID.randomUUID())
        }

        val response = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testLegalEntityJson)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertNotNull(responseBody["id"], "Response should contain an 'id' field")
    }

    @Test
    fun `POST fails if representative user does not exist`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testLegalEntityJson)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns a legal entity when it exists`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser()
        }

        val postResponse = client.post("/legal-entities") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testLegalEntityJson)
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
            insertMockedAdminUser(UUID.randomUUID())
        }

        val response = client.get("/legal-entities/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
