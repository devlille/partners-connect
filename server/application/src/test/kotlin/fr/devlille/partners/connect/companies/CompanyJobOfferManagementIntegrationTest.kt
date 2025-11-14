package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for company job offer management workflows.
 * Focus: End-to-end job offer business logic, data persistence, business rule enforcement.
 * Scope: Complete job offer scenarios including lifecycle, validation, and company relationships.
 */
class CompanyJobOfferManagementIntegrationTest {

    @Suppress("LongMethod")
    @Test
    fun `Complete job offer lifecycle for a company`() = testApplication {
        application { moduleMocked() }

        // Setup: Create company using factory
        val company = insertMockedCompany(name = "Tech Company")

        // Step 1: Create job offer with complete business data
        val jobOfferRequest = """
        {
            "title": "Senior Kotlin Developer",
            "description": "Join our team to build amazing applications with Kotlin and Ktor",
            "location": "Lille, France",
            "contract_type": "CDI",
            "salary_min": 55000,
            "salary_max": 75000,
            "remote_work": true,
            "experience_level": "SENIOR"
        }
        """.trimIndent()

        val createResponse = client.post("/companies/${company.id.value}/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(jobOfferRequest)
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdJobOffer = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
        val jobOfferId = createdJobOffer["id"]!!.jsonPrimitive.content

        // Verify business logic: job offer should be linked to company
        assertEquals("\"${company.id.value}\"", createdJobOffer["company_id"].toString())
        assertEquals("\"Senior Kotlin Developer\"", createdJobOffer["title"].toString())
        assertEquals("\"CDI\"", createdJobOffer["contract_type"].toString())
        assertNotNull(createdJobOffer["created_at"])

        // Step 2: Update job offer with business validation
        val updateRequest = """
        {
            "title": "Lead Kotlin Developer",
            "description": "Lead our development team",
            "salary_min": 70000,
            "salary_max": 90000,
            "experience_level": "LEAD"
        }
        """.trimIndent()

        val updateResponse = client.put("/companies/${company.id.value}/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedJobOffer = Json.parseToJsonElement(updateResponse.bodyAsText()).jsonObject

        // Verify business rules: salary range validation and title update
        assertEquals("\"Lead Kotlin Developer\"", updatedJobOffer["title"].toString())
        assertEquals("\"LEAD\"", updatedJobOffer["experience_level"].toString())
        assertEquals(70000, updatedJobOffer["salary_min"]!!.toString().toInt())
        assertEquals(90000, updatedJobOffer["salary_max"]!!.toString().toInt())

        // Step 3: Retrieve job offer and verify persistence
        val getResponse = client.get("/companies/${company.id.value}/job-offers/$jobOfferId")
        assertEquals(HttpStatusCode.OK, getResponse.status)

        val retrievedJobOffer = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals("\"Lead Kotlin Developer\"", retrievedJobOffer["title"].toString())
        assertEquals("\"${company.id.value}\"", retrievedJobOffer["company_id"].toString())

        // Step 4: Verify job offer appears in company's job offer listing
        val listResponse = client.get("/companies/${company.id.value}/job-offers")
        assertEquals(HttpStatusCode.OK, listResponse.status)

        val jobOffersJson = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val jobOffers = jobOffersJson["items"]!!.jsonArray
        assertEquals(1, jobOffers.size)
        assertEquals(jobOfferId, jobOffers[0].jsonObject["id"]!!.jsonPrimitive.content)

        // Step 5: Delete job offer and verify business logic
        val deleteResponse = client.delete("/companies/${company.id.value}/job-offers/$jobOfferId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify job offer no longer exists
        val getDeletedResponse = client.get("/companies/${company.id.value}/job-offers/$jobOfferId")
        assertEquals(HttpStatusCode.NotFound, getDeletedResponse.status)
    }

    @Test
    fun `Job offer salary validation enforces business rules`() = testApplication {
        application { moduleMocked() }

        val company = insertMockedCompany(name = "Salary Test Company")

        // Test: minimum salary cannot exceed maximum salary
        val invalidSalaryRequest = """
        {
            "title": "Test Position",
            "description": "Test description",
            "location": "Test Location",
            "contract_type": "CDI",
            "salary_min": 80000,
            "salary_max": 60000
        }
        """.trimIndent()

        val response = client.post("/companies/${company.id.value}/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(invalidSalaryRequest)
        }

        // Business rule validation: Should reject invalid salary range
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `Multiple job offers for same company are managed independently`() = testApplication {
        application { moduleMocked() }

        val company = insertMockedCompany(name = "Multi Job Company")

        // Create first job offer
        val firstJobRequest = """
        {
            "title": "Backend Developer",
            "description": "Backend development role",
            "location": "Paris",
            "contract_type": "CDI",
            "experience_level": "INTERMEDIATE"
        }
        """.trimIndent()

        val firstResponse = client.post("/companies/${company.id.value}/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(firstJobRequest)
        }

        assertEquals(HttpStatusCode.Created, firstResponse.status)
        val firstJobOffer = Json.parseToJsonElement(firstResponse.bodyAsText()).jsonObject
        val firstJobId = firstJobOffer["id"]!!.jsonPrimitive.content

        // Create second job offer
        val secondJobRequest = """
        {
            "title": "Frontend Developer",
            "description": "Frontend development role",
            "location": "Lyon",
            "contract_type": "CDD",
            "experience_level": "JUNIOR"
        }
        """.trimIndent()

        val secondResponse = client.post("/companies/${company.id.value}/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(secondJobRequest)
        }

        assertEquals(HttpStatusCode.Created, secondResponse.status)
        val secondJobOffer = Json.parseToJsonElement(secondResponse.bodyAsText()).jsonObject
        val secondJobId = secondJobOffer["id"]!!.jsonPrimitive.content

        // Verify both job offers exist and are independent
        val listResponse = client.get("/companies/${company.id.value}/job-offers")
        assertEquals(HttpStatusCode.OK, listResponse.status)

        val jobOffersJson = Json.parseToJsonElement(listResponse.bodyAsText()).jsonObject
        val jobOffers = jobOffersJson["items"]!!.jsonArray
        assertEquals(2, jobOffers.size)

        // Verify each job offer maintains its independent data
        val jobOfferIds = jobOffers.map { it.jsonObject["id"]!!.jsonPrimitive.content }
        assertTrue(jobOfferIds.contains(firstJobId))
        assertTrue(jobOfferIds.contains(secondJobId))

        // Update first job offer should not affect second
        val updateFirstRequest = """{"title": "Senior Backend Developer"}"""
        val updateResponse = client.put("/companies/${company.id.value}/job-offers/$firstJobId") {
            contentType(ContentType.Application.Json)
            setBody(updateFirstRequest)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify second job offer unchanged
        val secondJobResponse = client.get("/companies/${company.id.value}/job-offers/$secondJobId")
        val unchangedSecondJob = Json.parseToJsonElement(secondJobResponse.bodyAsText()).jsonObject
        assertEquals("\"Frontend Developer\"", unchangedSecondJob["title"].toString())
    }

    @Test
    fun `Job offer access is restricted to owning company`() = testApplication {
        application { moduleMocked() }

        val company1 = insertMockedCompany(name = "Company 1")
        val company2 = insertMockedCompany(name = "Company 2")

        // Create job offer for company 1
        val jobOffer = insertMockedJobOffer(
            companyId = company1.id.value,
            title = "Company 1 Job",
        )

        // Attempt to access job offer via company 2's endpoint
        val crossAccessResponse = client.get("/companies/${company2.id.value}/job-offers/${jobOffer.id.value}")

        // Business rule: Should not allow cross-company job offer access
        assertEquals(HttpStatusCode.NotFound, crossAccessResponse.status)

        // Verify correct access via company 1
        val correctAccessResponse = client.get("/companies/${company1.id.value}/job-offers/${jobOffer.id.value}")
        assertEquals(HttpStatusCode.OK, correctAccessResponse.status)
    }
}
