package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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

class EventUpdateRoutePut {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates an existing event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updateBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals(eventId.toString(), updateBody["slug"])
    }

    @Test
    fun `PUT returns 401 when user has no access to the event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val updateResponse = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)
    }
}
