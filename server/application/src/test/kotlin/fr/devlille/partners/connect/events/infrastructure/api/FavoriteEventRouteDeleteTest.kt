package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRouteDeleteTest {
    @Test
    fun `DELETE returns 204 when favorite exists`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-to-delete"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
                insertMockedFavoriteEvent(userId = userId, eventId = eventId)
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.delete("/users/me/favorite-events/anything")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction { insertMockedUser(userId) }
        }

        val response = client.delete("/users/me/favorite-events/does-not-exist") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE returns 404 when event exists but is not in the caller's favorites`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-not-favorited"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
                // NO insertMockedFavoriteEvent
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE returns 403 when event belongs to an org the caller has no permission on`() = testApplication {
        val userId = UUID.randomUUID()
        val ownerOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-other-org-delete"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(ownerOrgId)
                // NO insertMockedOrgaPermission for userId on ownerOrgId
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = ownerOrgId)
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
