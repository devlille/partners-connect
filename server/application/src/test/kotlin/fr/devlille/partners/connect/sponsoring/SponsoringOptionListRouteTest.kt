package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertTrue

class SponsoringOptionListRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns all options with all translations without Accept-Language header`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val request = CreateText(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Logo on website",
                    description = "Company logo displayed on event website",
                ),
                TranslatedLabel(
                    language = "fr",
                    name = "Logo sur le site web",
                    description = "Logo de l'entreprise affiché sur le site de l'événement",
                ),
                TranslatedLabel(language = "de", name = "Logo auf Website", description = ""),
            ),
            price = null,
        )

        client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // Intentionally NO Accept-Language header
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())
        assertTrue(responseBody.contains("translations"))
    }
}
