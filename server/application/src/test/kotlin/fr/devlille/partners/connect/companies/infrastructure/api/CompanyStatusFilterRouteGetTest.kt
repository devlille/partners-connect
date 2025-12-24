package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompanyStatusFilterRouteGetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET should return empty result when no companies match status filter`() = testApplication {
        application {
            moduleMocked()
            transaction {
                insertMockedCompany(UUID.randomUUID(), name = "Active Company 1", status = CompanyStatus.ACTIVE)
                insertMockedCompany(UUID.randomUUID(), name = "Active Company 2", status = CompanyStatus.ACTIVE)
            }
        }

        val response = client.get("/companies?filter[status]=inactive")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertEquals(0, result.total)
        assertEquals(0, result.items.size)
    }

    @Test
    fun `GET should return all companies by default`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
                insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
            }
        }

        val response = client.get("/companies")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertTrue { result.total > 0 }
    }

    @Test
    fun `GET should return only active companies when status=active`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
                insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
            }
        }

        val response = client.get("/companies?filter[status]=active")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertTrue { result.total > 0 }
        val active = result.items
            .filter { it.status == CompanyStatus.ACTIVE }
            .filter { it.name == "Active Company" }
        assertNotNull(active)
    }

    @Test
    fun `GET should return only inactive companies when status=inactive`() = testApplication {
        val activeCompanyId = UUID.randomUUID()
        val inactiveCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(activeCompanyId, name = "Active Company", status = CompanyStatus.ACTIVE)
                insertMockedCompany(inactiveCompanyId, name = "Inactive Company", status = CompanyStatus.INACTIVE)
            }
        }

        val response = client.get("/companies?filter[status]=inactive")

        assertEquals(HttpStatusCode.OK, response.status)

        val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
        assertTrue { result.total > 0 }
        val inactive = result.items
            .filter { it.status == CompanyStatus.INACTIVE }
            .filter { it.name == "Inactive Company" }
        assertNotNull(inactive)
    }

    @Test
    fun `GET should return 400 for invalid status parameter`() = testApplication {
        application {
            moduleSharedDb(userId = UUID.randomUUID())
        }

        val response = client.get("/companies?filter[status]=invalid_status")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET should support pagination with status filtering`() = testApplication {
        application {
            moduleMocked()
            transaction {
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
}
