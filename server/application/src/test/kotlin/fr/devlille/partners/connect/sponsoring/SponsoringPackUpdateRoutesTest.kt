package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SponsoringPackUpdateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT verifies updated pack via GET endpoint`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(
                    id = packId,
                    eventId = eventId,
                    basePrice = 1000,
                    maxQuantity = 5,
                )
            }
        }

        val updateRequest = createSponsoringPack(
            name = "Updated Pack Name",
            price = 3000,
            maxQuantity = 20,
        )

        // Update the pack
        client.put("/orgs/$orgId/events/$eventId/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(updateRequest))
        }

        // Verify via GET endpoint
        val getResponse = client.get("/orgs/$orgId/events/$eventId/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val packs = json.decodeFromString<List<SponsoringPack>>(getResponse.bodyAsText())
        assertEquals(1, packs.size)
        assertEquals("Updated Pack Name", packs[0].name)
        assertEquals(3000, packs[0].basePrice)
        assertEquals(20, packs[0].maxQuantity)
    }
}
