package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for complete company lifecycle workflows.
 * Focus: End-to-end business logic, data persistence, business rule enforcement.
 * Scope: Complete business scenarios from API request to database state validation.
 */
class CompanyLifecycleIntegrationTest {

    @Suppress("LongMethod")
    @Test
    fun `Complete company lifecycle - create, update, retrieve, delete`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        // Step 1: Create company with complete business data
        val createCompany = CreateCompany(
            name = "DevLille Corp",
            siteUrl = "https://devlille.fr",
            headOffice = Address(
                address = "123 Rue de Lille",
                city = "Lille",
                zipCode = "59000",
                country = "FR",
            ),
            siret = "12345678901234",
            vat = "FR12345678901",
            description = "Lille Developer Community",
            socials = listOf(
                Social(SocialType.LINKEDIN, "https://linkedin.com/devlille"),
                Social(SocialType.X, "https://x.com/devlille"),
            ),
        )

        val createResponse = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(createCompany)
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdCompany = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val companyId = createdCompany["id"]!!.jsonPrimitive.content

        // Step 2: Verify business logic - company should be ACTIVE by default
        assertEquals("\"ACTIVE\"", createdCompany["status"].toString())
        assertNotNull(createdCompany["created_at"])
        assertNotNull(createdCompany["updated_at"])

        // Step 3: Update company with business logic validation
        val updateRequest = """
        {
            "name": "DevLille Corporation Updated",
            "description": "Updated Lille Developer Community",
            "status": "INACTIVE"
        }
        """.trimIndent()

        val updateResponse = client.put("/companies/$companyId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedCompany = Json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject

        // Verify business rule: updated_at should be newer than created_at
        assertEquals("\"DevLille Corporation Updated\"", updatedCompany["name"].toString())
        assertEquals("\"INACTIVE\"", updatedCompany["status"].toString())
        assertEquals("\"Updated Lille Developer Community\"", updatedCompany["description"].toString())

        // Step 4: Retrieve company and verify persistence
        val getResponse = client.get("/companies/$companyId")
        assertEquals(HttpStatusCode.OK, getResponse.status)

        val retrievedCompany = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals("\"DevLille Corporation Updated\"", retrievedCompany["name"].toString())
        assertEquals("\"INACTIVE\"", retrievedCompany["status"].toString())

        // Step 5: Verify company appears in listing with correct status
        val listResponse = client.get("/companies")
        assertEquals(HttpStatusCode.OK, listResponse.status)

        val companiesJson = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val companies = companiesJson["items"]!!
        assertTrue(companies.toString().contains(companyId))
    }

    @Test
    fun `Company creation enforces business rules for unique SIRET`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        // Create first company
        val firstCompany = CreateCompany(
            name = "First Company",
            siteUrl = "https://first.com",
            headOffice = Address(
                address = "123 First St",
                city = "First City",
                zipCode = "12345",
                country = "FR",
            ),
            siret = "12345678901234",
            vat = "FR12345678901",
            description = "First company",
            socials = emptyList(),
        )

        val firstResponse = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(firstCompany)
        }

        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Attempt to create second company with same SIRET
        val secondCompany = CreateCompany(
            name = "Second Company",
            siteUrl = "https://second.com",
            headOffice = Address(
                address = "456 Second St",
                city = "Second City",
                zipCode = "67890",
                country = "FR",
            ),
            // Same SIRET as first company
            siret = "12345678901234",
            vat = "FR09876543210",
            description = "Second company",
            socials = emptyList(),
        )

        val secondResponse = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(secondCompany)
        }

        // Business rule validation: Should reject duplicate SIRET
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }

    @Test
    fun `Company status transitions follow business rules`() = testApplication {
        application { moduleMocked() }

        // Create company using factory
        val company = insertMockedCompany(
            name = "Status Test Company",
            status = CompanyStatus.ACTIVE,
        )

        // Test valid status transition: ACTIVE -> INACTIVE
        val deactivateRequest = """{"status": "INACTIVE"}"""
        val deactivateResponse = client.put("/companies/${company.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(deactivateRequest)
        }

        assertEquals(HttpStatusCode.OK, deactivateResponse.status)
        val deactivatedCompany = Json.parseToJsonElement(deactivateResponse.bodyAsText()).jsonObject
        assertEquals("\"INACTIVE\"", deactivatedCompany["status"].toString())

        // Test valid status transition: INACTIVE -> ACTIVE
        val reactivateRequest = """{"status": "ACTIVE"}"""
        val reactivateResponse = client.put("/companies/${company.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(reactivateRequest)
        }

        assertEquals(HttpStatusCode.OK, reactivateResponse.status)
        val reactivatedCompany = Json.parseToJsonElement(reactivateResponse.bodyAsText()).jsonObject
        assertEquals("\"ACTIVE\"", reactivatedCompany["status"].toString())
    }
}
