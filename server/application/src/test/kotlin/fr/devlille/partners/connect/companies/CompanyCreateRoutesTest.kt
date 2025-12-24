package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyCreateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a new company`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucketName",
            filename = "fileName",
            url = "https://example.com/original",
        )
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId, storage = storage)
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
}
