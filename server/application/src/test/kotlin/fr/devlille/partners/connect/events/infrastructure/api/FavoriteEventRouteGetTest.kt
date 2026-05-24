package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRouteGetTest {
    @Test
    fun `GET returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.get("/users/me/favorite-events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 200 with empty array when caller has no favorites`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction { insertMockedUser(userId) }
        }

        val response = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns favorites in two orgs ordered by start_time ascending`() = testApplication {
        val userId = UUID.randomUUID()
        val orgAId = UUID.randomUUID()
        val orgBId = UUID.randomUUID()
        val laterEventId = UUID.randomUUID()
        val earlierEventId = UUID.randomUUID()
        val laterSlug = "event-later"
        val earlierSlug = "event-earlier"
        val laterStart = LocalDateTime.parse("2030-12-01T00:00:00")
        val earlierStart = LocalDateTime.parse("2030-06-01T00:00:00")

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgAId)
                insertMockedOrganisationEntity(orgBId)
                insertMockedOrgaPermission(orgAId, userId = userId)
                insertMockedOrgaPermission(orgBId, userId = userId)
                insertMockedFutureEventWithSlug(id = laterEventId, slug = laterSlug, orgId = orgAId).apply {
                    startTime = laterStart
                }
                insertMockedFutureEventWithSlug(id = earlierEventId, slug = earlierSlug, orgId = orgBId).apply {
                    startTime = earlierStart
                }
                insertMockedFavoriteEvent(userId = userId, eventId = laterEventId)
                insertMockedFavoriteEvent(userId = userId, eventId = earlierEventId)
            }
        }

        val response = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, items.size)
        assertEquals(earlierSlug, items[0].jsonObject["slug"]?.jsonPrimitive?.content)
        assertEquals(laterSlug, items[1].jsonObject["slug"]?.jsonPrimitive?.content)
    }
}
