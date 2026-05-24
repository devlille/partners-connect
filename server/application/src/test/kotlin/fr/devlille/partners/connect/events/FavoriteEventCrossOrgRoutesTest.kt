package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventsTable
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventCrossOrgRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `user A can favorite own org event, cannot favorite other org event, isolated from user B`() = testApplication {
        val userAId = UUID.randomUUID()
        val userBId = UUID.randomUUID()
        val orgAId = UUID.randomUUID()
        val orgBId = UUID.randomUUID()
        val eventAId = UUID.randomUUID()
        val eventBId = UUID.randomUUID()
        val eventASlug = "event-org-a"
        val eventBSlug = "event-org-b"

        // The mock OAuth engine resolves "Bearer valid" to userAId here.
        application {
            moduleSharedDb(userId = userAId)
            transaction {
                insertMockedUser(userAId)
                insertMockedUser(userBId)
                insertMockedOrganisationEntity(orgAId)
                insertMockedOrganisationEntity(orgBId)
                insertMockedOrgaPermission(orgAId, userId = userAId)
                insertMockedOrgaPermission(orgBId, userId = userBId)
                insertMockedFutureEventWithSlug(id = eventAId, slug = eventASlug, orgId = orgAId)
                insertMockedFutureEventWithSlug(id = eventBId, slug = eventBSlug, orgId = orgBId)
            }
        }

        // Step 1: user A favorites event-A → 201
        val addAResp = client.put("/users/me/favorite-events/$eventASlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Created, addAResp.status)

        // Step 2: user A lists favorites → contains event-A only
        val listResp = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, listResp.status)
        val items = Json.parseToJsonElement(listResp.bodyAsText()).jsonArray
        assertEquals(1, items.size)
        assertEquals(eventASlug, items[0].jsonObject["slug"]?.jsonPrimitive?.content)

        // Step 3: user A tries to favorite event-B (org B, no permission) → 403
        val crossOrgResp = client.put("/users/me/favorite-events/$eventBSlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Forbidden, crossOrgResp.status)

        // Step 4: user B's favorites are isolated from user A's — verified at the DB layer
        //         (the mock OAuth engine is fixed to userAId; we can't switch identity mid-test,
        //         so we assert state directly).
        transaction {
            val rowsForUserB = FavoriteEventEntity
                .find { FavoriteEventsTable.userId eq userBId }
                .count()
            assertEquals(0, rowsForUserB, "User B should have zero favorites")

            val rowsForUserA = FavoriteEventEntity
                .find { FavoriteEventsTable.userId eq userAId }
                .count()
            assertEquals(1, rowsForUserA, "User A should have exactly one favorite")
        }

        // Step 5: user A removes their favorite → 204
        val deleteResp = client.delete("/users/me/favorite-events/$eventASlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResp.status)

        // Step 6: user A lists favorites again → empty
        val finalListResp = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, finalListResp.status)
        assertEquals("[]", finalListResp.bodyAsText())
    }
}
