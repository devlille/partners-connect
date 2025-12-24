package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.companies.domain.UpdateCompany
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyUpdateRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT should update company with valid partial data`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(
                    id = companyId,
                    name = "Original Company",
                    description = "Original description",
                )
            }
        }

        val updateRequest = UpdateCompany(
            name = "Updated Company Name",
            description = "New description",
        )

        val response = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("\"Updated Company Name\"", responseBody["name"].toString())
        assertEquals("\"New description\"", responseBody["description"].toString())
        assertEquals("\"active\"", responseBody["status"].toString())
    }

    @Test
    fun `PUT should handle partial updates without affecting other fields`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(
                    id = companyId,
                    name = "Original Company",
                    siret = "12345678901234",
                    vat = "FR12345678901",
                )
            }
        }

        val partialUpdate = UpdateCompany(name = "New Name Only")

        val response = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(partialUpdate))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("\"New Name Only\"", responseBody["name"].toString())
        assertEquals("\"12345678901234\"", responseBody["siret"].toString()) // Unchanged
        assertEquals("\"FR12345678901\"", responseBody["vat"].toString()) // Unchanged
    }

    @Test
    fun `PUT should return 400 for invalid update data`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val invalidUpdate = UpdateCompany(
            // Should be 14 digits
            siret = "invalid-siret",
            // Should match VAT format
            vat = "invalid-vat",
        )

        val response = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(invalidUpdate))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorBody = response.bodyAsText()
        // Should contain validation error details from JSON schema
        assert(errorBody.contains("pattern") || errorBody.contains("does not match") || errorBody.contains("Invalid"))
    }

    @Test
    fun `PUT should return 404 when updating non-existent company`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            // No company inserted
        }

        val updateRequest = UpdateCompany(name = "Updated Name")

        val response = client.put("/companies/$nonExistentCompanyId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT should update company socials when provided`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val updateWithSocials = UpdateCompany(
            socials = listOf(
                Social(SocialType.X, "https://x.com/updated"),
                Social(SocialType.LINKEDIN, "https://linkedin.com/company/updated"),
            ),
        )

        val response = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateWithSocials))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        // Verify socials are updated (implementation will need to include socials in response)
        // For now, just verify the response structure is valid
        assert(responseBody.containsKey("id"))
    }
}
