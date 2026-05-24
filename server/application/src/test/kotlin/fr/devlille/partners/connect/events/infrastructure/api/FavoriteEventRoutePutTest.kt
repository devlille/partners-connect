package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRoutePutTest {
    @Test
    fun `PUT returns 201 on first add`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-a"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
            }
        }

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `PUT returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.put("/users/me/favorite-events/anything")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        val response = client.put("/users/me/favorite-events/does-not-exist") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 403 when event belongs to an org the caller has no permission on`() = testApplication {
        val userId = UUID.randomUUID()
        val ownerOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-other-org"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(ownerOrgId)
                // NO insertMockedOrgaPermission for userId on ownerOrgId
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = ownerOrgId)
            }
        }

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT returns 409 when caller has already favorited this event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-already-favorited"

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

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
