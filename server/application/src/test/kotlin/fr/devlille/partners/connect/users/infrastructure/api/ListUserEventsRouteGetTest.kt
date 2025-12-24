package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ListUserEventsRouteGetTest {
    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/users/me/events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 403 if authenticated user has no organizer permissions`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return empty array if organizer has no events`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
            }
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }
}
