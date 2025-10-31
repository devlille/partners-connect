package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyStatusFilterIntegrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET should return all companies by default`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
            insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
        }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(2, result.total)
        // Both active and inactive companies should be returned by default
        val companyNames = result.items.map { it.name }.sorted()
        assertEquals(listOf("Active Company", "Inactive Company"), companyNames)
    }

    @Test
    fun `GET should return only active companies when status=active`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
            insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
        }

        val response = client.get("/companies?filter[status]=active")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(1, result.total)
        assertEquals("Active Company", result.items[0].name)
        assertEquals(CompanyStatus.ACTIVE, result.items[0].status)
    }

    @Test
    fun `GET should return only inactive companies when status=inactive`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
            insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
        }

        val response = client.get("/companies?filter[status]=inactive")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(1, result.total)
        assertEquals("Inactive Company", result.items[0].name)
        assertEquals(CompanyStatus.INACTIVE, result.items[0].status)
    }

    @Test
    fun `GET should combine query search with status filtering`() = testApplication {
        application {
            moduleMocked()
            insertMockedCompany(UUID.randomUUID(), name = "Tech Active Company", status = CompanyStatus.ACTIVE)
            insertMockedCompany(UUID.randomUUID(), name = "Tech Inactive Company", status = CompanyStatus.INACTIVE)
            insertMockedCompany(UUID.randomUUID(), name = "Other Active Company", status = CompanyStatus.ACTIVE)
        }

        val response = client.get("/companies?query=tech&filter[status]=active")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(1, result.total)
        assertEquals("Tech Active Company", result.items[0].name)
        assertEquals(CompanyStatus.ACTIVE, result.items[0].status)
    }

    @Test
    fun `GET should return 400 for invalid status parameter`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/companies?filter[status]=invalid_status")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET should support pagination with status filtering`() = testApplication {
        application {
            moduleMocked()
            // Create multiple active companies for pagination testing
            repeat(15) { index ->
                insertMockedCompany(
                    UUID.randomUUID(),
                    name = "Active Company $index",
                    status = CompanyStatus.ACTIVE,
                )
            }
            // Create some inactive companies too
            repeat(10) { index ->
                insertMockedCompany(
                    UUID.randomUUID(),
                    name = "Inactive Company $index",
                    status = CompanyStatus.INACTIVE,
                )
            }
        }

        val response = client.get("/companies?filter[status]=active&page=1&page_size=10")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(10, result.items.size) // Should return only 10 items per page
        assertEquals(15, result.total) // Total should be 15 active companies
        assertEquals(1, result.page)
        assertEquals(10, result.pageSize)
        // Verify all returned companies are active
        result.items.forEach { company ->
            assertEquals(CompanyStatus.ACTIVE, company.status)
            assert(company.name.startsWith("Active Company"))
        }
    }

    @Test
    fun `GET should combine query search with inactive status filtering`() = testApplication {
        application {
            moduleMocked()
            insertMockedCompany(UUID.randomUUID(), name = "Tech Active Company", status = CompanyStatus.ACTIVE)
            insertMockedCompany(UUID.randomUUID(), name = "Tech Inactive Company", status = CompanyStatus.INACTIVE)
            insertMockedCompany(UUID.randomUUID(), name = "Other Inactive Company", status = CompanyStatus.INACTIVE)
        }

        val response = client.get("/companies?query=tech&filter[status]=inactive")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(1, result.total)
        assertEquals("Tech Inactive Company", result.items[0].name)
        assertEquals(CompanyStatus.INACTIVE, result.items[0].status)
    }

    @Test
    fun `GET should return empty result when no companies match status filter`() = testApplication {
        application {
            moduleMocked()
            // Only create active companies
            insertMockedCompany(UUID.randomUUID(), name = "Active Company 1", status = CompanyStatus.ACTIVE)
            insertMockedCompany(UUID.randomUUID(), name = "Active Company 2", status = CompanyStatus.ACTIVE)
        }

        val response = client.get("/companies?filter[status]=inactive")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(0, result.total)
        assertEquals(0, result.items.size)
    }
}
