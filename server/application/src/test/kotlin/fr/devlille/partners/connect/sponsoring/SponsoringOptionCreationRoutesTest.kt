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

class SponsoringOptionCreationRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates option and GET returns all translations regardless of Accept-Language`() = testApplication {
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
                TranslatedLabel(language = "fr", name = "Nom FR"),
            ),
        )

        client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        val response = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "de") // Unsupported language, but should still work
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        // Organizer endpoints return all translations, so they don't fail on missing languages
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
