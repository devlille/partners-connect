package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
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

class EventQandaConfigRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates event with Q&A config enabled and returns 200`() = testApplication {
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

        val event = createEvent(
            qandaEnabled = true,
            qandaMaxQuestions = 3,
            qandaMaxAnswers = 4,
        )
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT updates event with Q&A disabled and returns 200`() = testApplication {
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

        val event = createEvent(qandaEnabled = false)
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 400 when Q&A enabled with invalid max_answers`() = testApplication {
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

        val event = createEvent(
            qandaEnabled = true,
            qandaMaxQuestions = 3,
            qandaMaxAnswers = 1,
        )
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 400 when Q&A enabled without max limits`() = testApplication {
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

        val event = createEvent(
            qandaEnabled = true,
            qandaMaxQuestions = null,
            qandaMaxAnswers = null,
        )
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
