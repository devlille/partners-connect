package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyJobOfferRouteDeleteTest {
    @Test
    fun `DELETE job offer should remove job offer and return 204`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
                insertMockedJobOffer(companyId, jobOfferId)
            }
        }

        val response = client.delete("/companies/$companyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE job offer with invalid UUID should return 400`() = testApplication {
        val companyId = UUID.randomUUID()
        val invalidJobOfferId = "invalid-uuid"

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val response = client.delete("/companies/$companyId/job-offers/$invalidJobOfferId")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE non-existent job offer should return 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val response = client.delete("/companies/$companyId/job-offers/$nonExistentJobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE job offer for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            // Do not insert company
        }

        val response = client.delete("/companies/$nonExistentCompanyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE job offer from different company should return 404`() = testApplication {
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId1)
                insertMockedCompany(companyId2)
                insertMockedJobOffer(companyId1, jobOfferId)
            }
        }

        // Try to delete job offer from companyId1 via companyId2's endpoint
        val response = client.delete("/companies/$companyId2/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
