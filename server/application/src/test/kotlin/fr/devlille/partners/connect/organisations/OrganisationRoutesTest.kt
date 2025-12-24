package fr.devlille.partners.connect.organisations

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.factories.createOrganisation
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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

class OrganisationRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns organisation with null fields when minimal data`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        // Create a minimal organisation
        val postResponse = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation(name = "Minimal Test Org")))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        val slug = postResponseBody["slug"]!!

        val getResponse = client.get("/orgs/$slug")

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val organisation = Json.decodeFromString<Organisation>(getResponse.bodyAsText())
        assertEquals("Minimal Test Org", organisation.name)
        assertEquals(null, organisation.headOffice)
        assertEquals(null, organisation.iban)
        assertEquals(null, organisation.bic)
        assertEquals(null, organisation.representativeUserEmail)
    }

    @Test
    fun `GET returns a organisation when it exists`() = testApplication {
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                val org = insertMockedOrganisationEntity()
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = org.id.value, userId = userId)
            }
        }

        val postResponse = client.post("/orgs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Organisation.serializer(), createOrganisation()))
        }
        val postResponseBody = Json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())

        val getResponse = client.get("/orgs/${postResponseBody["slug"]}")

        assertEquals(HttpStatusCode.OK, getResponse.status)
    }
}
