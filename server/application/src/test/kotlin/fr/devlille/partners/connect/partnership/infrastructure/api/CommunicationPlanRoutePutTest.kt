package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedCommunicationPlan
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class CommunicationPlanRoutePutTest {
    @Test
    fun `PUT updates a communication plan entry`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val entryId = UUID.randomUUID()

        val now = Clock.System.now()
        val scheduledDate = now.plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCommunicationPlan(
                    id = entryId,
                    eventId = eventId,
                    title = "Original title",
                    scheduledDate = scheduledDate,
                )
            }
        }

        val newDate = now.plus(duration = 5.days).toLocalDateTime(TimeZone.UTC)
        val response = client.put("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"title":"Updated title","scheduled_date":"$newDate"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Updated title", body["title"]!!.jsonPrimitive.content)
        assertEquals(entryId.toString(), body["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `PUT returns 400 when title is blank`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val entryId = UUID.randomUUID()

        val now = Clock.System.now()
        val scheduledDate = now.plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCommunicationPlan(
                    id = entryId,
                    eventId = eventId,
                    title = "Title",
                    scheduledDate = scheduledDate,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"title":"","scheduled_date":"$scheduledDate"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 401 without authorization`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val entryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCommunicationPlan(id = entryId, eventId = eventId, title = "Title")
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Updated","scheduled_date":"2026-06-15T10:00:00"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 for unauthorized organization`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val otherOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val entryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrganisationEntity(otherOrgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = otherOrgId)
                insertMockedCommunicationPlan(id = entryId, eventId = eventId, title = "Title")
            }
        }

        val response = client.put("/orgs/$otherOrgId/events/$eventId/communication-plan/$entryId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"title":"Updated","scheduled_date":"2026-06-15T10:00:00"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 for unknown entry id`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val unknownId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/communication-plan/$unknownId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"title":"Updated","scheduled_date":"2026-06-15T10:00:00"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 for entry belonging to a different event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val otherEventId = UUID.randomUUID()
        val entryId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedFutureEvent(otherEventId, orgId = orgId)
                insertMockedCommunicationPlan(id = entryId, eventId = otherEventId, title = "Title")
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"title":"Updated","scheduled_date":"2026-06-15T10:00:00"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
