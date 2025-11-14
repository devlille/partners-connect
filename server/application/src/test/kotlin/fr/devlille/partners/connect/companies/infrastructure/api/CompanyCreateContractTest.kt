package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company creation API endpoint.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyCreateContractTest {

    @Test
    fun `POST companies returns 201 with valid company data structure`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val validCompanyRequest = CreateCompany(
            name = "Test Company",
            siteUrl = "https://test.com",
            headOffice = Address(
                address = "123 Test St",
                city = "Test City",
                zipCode = "12345",
                country = "FR",
            ),
            siret = "12345678901234",
            vat = "FR12345678901",
            description = "Test description",
            socials = listOf(Social(SocialType.LINKEDIN, "https://linkedin.com/test")),
        )

        val response = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(validCompanyRequest)
        }

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.Created, response.status)

        // Contract validation: Response structure contains required fields
        val responseBody = response.bodyAsText()
        val companyJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response schema structure
        assertTrue(companyJson.containsKey("id"))
        assertTrue(companyJson.containsKey("name"))
        assertTrue(companyJson.containsKey("site_url"))
        assertTrue(companyJson.containsKey("head_office"))
        assertTrue(companyJson.containsKey("siret"))
        assertTrue(companyJson.containsKey("vat"))
        assertTrue(companyJson.containsKey("description"))
        assertTrue(companyJson.containsKey("socials"))
        assertTrue(companyJson.containsKey("status"))

        // Verify nested head_office structure
        val headOffice = companyJson["head_office"]!!.jsonObject
        assertTrue(headOffice.containsKey("address"))
        assertTrue(headOffice.containsKey("city"))
        assertTrue(headOffice.containsKey("zip_code"))
        assertTrue(headOffice.containsKey("country"))
    }

    @Test
    fun `POST companies returns 400 with invalid request structure`() = testApplication {
        application { moduleMocked() }

        val invalidRequest = """{"invalid": "structure"}"""

        val response = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(invalidRequest)
        }

        // Contract validation: Bad request status for invalid schema
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST companies returns 400 with missing required fields`() = testApplication {
        application { moduleMocked() }

        val incompleteRequest = """{"name": "Test"}"""

        val response = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(incompleteRequest)
        }

        // Contract validation: Bad request status for incomplete data
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
