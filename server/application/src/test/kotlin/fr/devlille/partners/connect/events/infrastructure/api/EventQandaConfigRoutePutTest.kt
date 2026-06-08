package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    fun `PUT updates event with Q&A submission deadline and returns 200`() = testApplication {
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
            qandaSubmissionDeadline = LocalDateTime.parse("2026-12-01T18:30:15"),
        )
        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val getResponse = client.get("/events/$eventId")
        val body = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        val qandaConfig = body["event"]!!.jsonObject["qanda_config"]?.jsonObject
        assertNotNull(qandaConfig)
        assertEquals(
            "2026-12-01T18:30:15",
            qandaConfig["submission_deadline"]!!.jsonPrimitive.content,
        )
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

    @Test
    fun `PUT with Q&A disabled clears a previously set submission deadline`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                event.qandaSubmissionDeadline = LocalDateTime.parse("2026-12-01T18:30:15")
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val event = createEvent(qandaEnabled = false)
        val putResponse = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(event))
        }
        assertEquals(HttpStatusCode.OK, putResponse.status)

        val getResponse = client.get("/events/$eventId")
        val body = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        val eventObj = body["event"]!!.jsonObject
        assertTrue(
            !eventObj.containsKey("qanda_config") ||
                eventObj["qanda_config"] is kotlinx.serialization.json.JsonNull,
        )
    }
}
