package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.PromoteJobOfferRequest
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PromoteJobOfferRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST promote returns 409 when promotion already exists with status pending`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
            }
        }

        // First promotion
        val input = PromoteJobOfferRequest(jobOfferId.toString())
        client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        // Attempt duplicate promotion
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
