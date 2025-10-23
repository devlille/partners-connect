package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.UpdateJobOffer
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyJobOfferRoutesUpdateTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT job offer should update job offer and return updated data`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
            insertMockedJobOffer(companyId, jobOfferId)
        }

        val updateData = UpdateJobOffer(
            title = "Lead Kotlin Developer",
            salary = "70000-85000 EUR",
            experienceYears = 7,
        )

        val response = client.put("/companies/$companyId/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateData))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val jobOffer = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertTrue(jobOffer.containsKey("id"))
        assertTrue(jobOffer.containsKey("updated_at"))

        assertEquals("Lead Kotlin Developer", jobOffer["title"]?.jsonPrimitive?.content)
        assertEquals("70000-85000 EUR", jobOffer["salary"]?.jsonPrimitive?.content)
        assertEquals(7, jobOffer["experience_years"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `PUT job offer with partial update should update only specified fields`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
            insertMockedJobOffer(companyId, jobOfferId)
        }

        val partialUpdate = UpdateJobOffer(
            title = "Senior Kotlin Developer",
            // Only updating title
        )

        val response = client.put("/companies/$companyId/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(partialUpdate))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val jobOffer = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partialUpdate.title, jobOffer["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `PUT non-existent job offer should return 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val updateData = UpdateJobOffer(title = "Updated Title")
        val response = client.put("/companies/$companyId/job-offers/$nonExistentJobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateJobOffer.serializer(), updateData))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT job offer for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            // Do not insert company
        }

        val updateData = UpdateJobOffer(title = "Updated Title")
        val response = client.put("/companies/$nonExistentCompanyId/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateJobOffer.serializer(), updateData))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT job offer with invalid data should return 400`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val invalidUpdate = UpdateJobOffer(
            // Above maximum of 20
            experienceYears = 25,
        )

        val response = createClient {
            install(ContentNegotiation) {
                json()
            }
        }.put("/companies/$companyId/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(invalidUpdate)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT job offer with empty body should return 400`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val emptyUpdate = mapOf<String, String>()

        val response = createClient {
            install(ContentNegotiation) {
                json()
            }
        }.put("/companies/$companyId/job-offers/$jobOfferId") {
            contentType(ContentType.Application.Json)
            setBody(emptyUpdate)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
