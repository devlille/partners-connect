package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipUpdateRoutesTest {
    @Test
    fun `PUT accepts all valid language codes`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val validLanguages = listOf("en", "fr", "de", "nl", "es")
        val partnershipIds = validLanguages.map { UUID.randomUUID() }

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                partnershipIds.forEach { partnershipId ->
                    insertMockedPartnership(
                        id = partnershipId,
                        eventId = eventId,
                        companyId = companyId,
                        selectedPackId = packId,
                    )
                }
            }
        }

        // Test each language code
        validLanguages.zip(partnershipIds).forEach { (lang, partnershipId) ->
            val response = client.put("/events/$eventId/partnerships/$partnershipId") {
                contentType(ContentType.Application.Json)
                setBody("""{"language": "$lang"}""")
            }

            assertEquals(
                HttpStatusCode.OK,
                response.status,
                "Language $lang should be accepted",
            )
        }
    }
}
