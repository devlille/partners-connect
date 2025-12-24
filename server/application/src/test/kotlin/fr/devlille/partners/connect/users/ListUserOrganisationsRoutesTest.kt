package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUserOrganisationsRoutesTest {
    @Test
    fun `return multiple organizations for organizer with multiple memberships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        val email = "john.doe.$userId@contact.com"

        application {
            moduleSharedDb(userId)
            transaction {
                val user = insertMockedUser(userId, email = email)

                // First organization
                insertMockedOrganisationEntity(
                    id = orgId1,
                    name = "GDG Lille",
                    headOffice = "74 Rue des Beaux Arts, 59000 Lille",
                    representativeUser = user,
                )
                insertMockedOrgaPermission(orgId = orgId1, userId = userId, canEdit = true)

                // Second organization
                insertMockedOrganisationEntity(
                    id = orgId2,
                    name = "Developers Lille",
                    headOffice = "1 Place de la République, 59000 Lille",
                    representativeUser = user,
                )
                insertMockedOrgaPermission(orgId = orgId2, userId = userId, canEdit = true)
            }
        }

        val response = client.get("/users/me/orgs") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val orgsArray = json.jsonArray

        assertEquals(2, orgsArray.size)

        val orgSlugs = orgsArray.map { it.jsonObject["slug"]?.jsonPrimitive?.content }
        assertTrue(orgSlugs.contains("gdg-lille"))
        assertTrue(orgSlugs.contains("developers-lille"))
    }
}
