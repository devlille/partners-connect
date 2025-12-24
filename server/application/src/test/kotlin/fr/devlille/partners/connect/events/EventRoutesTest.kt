package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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

class EventRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns 400 for invalid page or pageSize`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId)
                insertMockedFutureEvent(orgId = orgId)
            }
        }

        val invalidParams = listOf(
            "page=0",
            "page=-1",
            "page_size=0",
            "page_size=-5",
        )

        for (param in invalidParams) {
            val response = client.get("/orgs/$orgId/events?$param") {
                header(HttpHeaders.Authorization, "Bearer valid")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status, "Should return 400 for param: $param")
        }
    }

    @Test
    fun `POST returns 409 when event already exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val updateResponse = client.post("/orgs/$orgId/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent(name = eventId.toString())))
        }

        assertEquals(HttpStatusCode.Created, updateResponse.status)

        val updateResponse2 = client.post("/orgs/$orgId/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent(name = eventId.toString())))
        }

        assertEquals(HttpStatusCode.Conflict, updateResponse2.status)
    }
}
