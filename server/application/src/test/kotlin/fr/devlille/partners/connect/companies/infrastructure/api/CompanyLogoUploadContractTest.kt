package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company logo upload API endpoint.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyLogoUploadContractTest {

    @Test
    fun `POST company logo upload returns 200 with valid upload response structure`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/logo.jpg",
        )

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        // Minimal setup using factory function
        val company = insertMockedCompany(name = "Test Company")

        // Create a temporary image file for upload testing
        val tempFile = File.createTempFile("test", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3, 4)) // Minimal file content

        try {
            val response = client.submitFormWithBinaryData(
                url = "/companies/${company.id.value}/logo",
                formData = formData {
                    append(
                        "logo",
                        tempFile.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=logo.jpg")
                        },
                    )
                },
            )

            // Contract validation: HTTP status code
            assertEquals(HttpStatusCode.OK, response.status)

            // Contract validation: Response structure contains upload result
            val responseBody = response.bodyAsText()
            val uploadJson = Json.parseToJsonElement(responseBody).jsonObject

            // Verify response schema structure for logo upload
            assertTrue(uploadJson.containsKey("id"))
            assertTrue(uploadJson.containsKey("logo_url_original"))
            assertTrue(uploadJson.containsKey("logo_url_1000"))
            assertTrue(uploadJson.containsKey("logo_url_500"))
            assertTrue(uploadJson.containsKey("logo_url_250"))
            assertTrue(uploadJson.containsKey("updated_at"))

            // Verify ID consistency
            assertEquals("\"${company.id.value}\"", uploadJson["id"].toString())
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `POST company logo with non-existent ID returns 404`() = testApplication {
        val storage = mockk<Storage>()
        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val nonExistentId = UUID.randomUUID()
        val tempFile = File.createTempFile("test", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        try {
            val response = client.submitFormWithBinaryData(
                url = "/companies/$nonExistentId/logo",
                formData = formData {
                    append(
                        "logo",
                        tempFile.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=logo.jpg")
                        },
                    )
                },
            )

            // Contract validation: Not found status for non-existent resource
            assertEquals(HttpStatusCode.NotFound, response.status)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `POST company logo with invalid ID format returns 400`() = testApplication {
        val storage = mockk<Storage>()
        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val invalidId = "not-a-uuid"
        val tempFile = File.createTempFile("test", ".jpg")
        tempFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        try {
            val response = client.submitFormWithBinaryData(
                url = "/companies/$invalidId/logo",
                formData = formData {
                    append(
                        "logo",
                        tempFile.readBytes(),
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=logo.jpg")
                        },
                    )
                },
            )

            // Contract validation: Bad request status for invalid ID format
            assertEquals(HttpStatusCode.BadRequest, response.status)
        } finally {
            tempFile.delete()
        }
    }
}
