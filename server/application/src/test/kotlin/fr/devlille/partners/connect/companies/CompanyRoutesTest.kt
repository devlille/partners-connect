package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns empty list if no companies exist`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val paginated = Json.parseToJsonElement(responseBody).jsonObject
        val items = paginated["items"]!!.jsonArray
        assertEquals(0, items.size)
        assertEquals(1, paginated["page"]!!.toString().toInt())
        assertEquals(20, paginated["page_size"]!!.toString().toInt())
        assertEquals(0, paginated["total"]!!.toString().toInt())
    }

    @Test
    fun `POST creates a new company`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val input = CreateCompany(
            name = "DevLille",
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
            socials = listOf(Social(SocialType.LINKEDIN, "https://linkedin.com/devlille")),
        )

        val createdResponse = client.post("/companies") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateCompany.serializer(), input))
        }
        val id = json.decodeFromString<Map<String, String>>(createdResponse.bodyAsText())["id"]

        assertEquals(HttpStatusCode.Created, createdResponse.status)

        val response = client.get("/companies")
        assertEquals(HttpStatusCode.OK, response.status)
        val paginated = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertTrue(paginated.items.any { it.toString().contains(id!!) })
        assertEquals(1, paginated.items.first().socials.size)
        assertEquals(SocialType.LINKEDIN, paginated.items.first().socials.first().type)
        assertEquals(1, paginated.total)
    }

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
            insertMockedCompany(companyId)
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
            moduleMocked()
            insertMockedCompany(companyId)
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

    @Test
    fun `GET returns companies sorted by name`() = testApplication {
        application {
            moduleMocked()
            listOf("Zeta", "Alpha", "Beta").forEach {
                insertMockedCompany(name = it, description = it)
            }
        }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val paginated = Json.parseToJsonElement(responseBody).jsonObject
        val items = paginated["items"]!!.jsonArray
        val names = items.map { it.jsonObject["name"]!!.toString() }
        val alphaIdx = names.indexOf("\"Alpha\"")
        val betaIdx = names.indexOf("\"Beta\"")
        val zetaIdx = names.indexOf("\"Zeta\"")
        assertTrue(alphaIdx < betaIdx)
        assertTrue(betaIdx < zetaIdx)
    }
}
