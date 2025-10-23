package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.CreateJobOffer
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyJobOfferRoutesCreateTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST job offer should create job offer and return 201 with ID`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val jobOffer = CreateJobOffer(
            url = "https://example.com/jobs/kotlin-developer",
            title = "Senior Kotlin Developer",
            location = "Lille, France",
            publicationDate = LocalDateTime.parse("2025-10-16T09:00:00"),
            endDate = LocalDateTime.parse("2025-12-15T23:59:00"),
            experienceYears = 5,
            salary = "60000-70000 EUR",
        )

        val response = client.post("/companies/$companyId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(jobOffer))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody.containsKey("id"))

        // Verify the ID is a valid UUID
        val jobOfferId = responseBody["id"]?.jsonPrimitive?.content
        UUID.fromString(jobOfferId) // Should not throw exception
    }

    @Test
    fun `POST job offer with missing required fields should return 400`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val incompleteJobOffer = mapOf(
            "title" to "Developer",
            // Missing url, location, publicationDate
        )

        val response = client.post("/companies/$companyId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(incompleteJobOffer))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST job offer with invalid experience years should return 400`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val invalidJobOffer = CreateJobOffer(
            url = "https://example.com/jobs/developer",
            title = "Developer",
            location = "Paris, France",
            publicationDate = LocalDateTime.parse("2025-10-16T09:00:00"),
            // Above maximum of 20
            experienceYears = 25,
        )

        val response = client.post("/companies/$companyId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(invalidJobOffer))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST job offer with future publication date should return 400`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val futureJobOffer = CreateJobOffer(
            url = "https://example.com/jobs/developer",
            title = "Developer",
            location = "Paris, France",
            // Future date
            publicationDate = LocalDateTime.parse("2026-01-01T09:00:00"),
        )

        val response = client.post("/companies/$companyId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(futureJobOffer))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST job offer for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()

        application {
            moduleMocked()
            // Do not insert company
        }

        val jobOffer = CreateJobOffer(
            url = "https://example.com/jobs/developer",
            title = "Developer",
            location = "Paris, France",
            publicationDate = LocalDateTime.parse("2025-10-16T09:00:00"),
        )

        val response = client.post("/companies/$nonExistentCompanyId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(jobOffer))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
