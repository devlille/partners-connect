package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyUploadLogoRoutePostTest {
    @Test
    fun `POST logo uploads and processes an SVG`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )
        val companyId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/companies/$companyId/logo",
            formData = formData {
                append(
                    "file",
                    File("src/test/resources/devlille-logo.svg").readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/svg+xml")
                        append(HttpHeaders.ContentDisposition, "filename=devlille-logo.svg")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("original"))
        assertTrue(body.contains("png_1000"))
    }

    @Test
    fun `POST logo rejects unsupported file type`() = testApplication {
        val companyId = UUID.randomUUID()
        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/companies/$companyId/logo",
            formData = formData {
                append(
                    "file",
                    "some unknown content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/pdf")
                        append(HttpHeaders.ContentDisposition, "filename=brochure.pdf")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported file type"))
    }
}
