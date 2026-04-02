package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventQandaConfigRouteGetTest {
    @Test
    fun `GET event returns qanda_config null when Q&A disabled`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val eventObj = body["event"]!!.jsonObject
        assertTrue(
            !eventObj.containsKey("qanda_config") ||
                eventObj["qanda_config"] is kotlinx.serialization.json.JsonNull,
        )
    }

    @Test
    fun `GET event returns qanda_config when Q&A enabled via org route`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 3
                event.qandaMaxAnswers = 4
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val eventObj = body["event"]!!.jsonObject
        val qandaConfig = eventObj["qanda_config"]?.jsonObject
        assertNotNull(qandaConfig)
        assertEquals("3", qandaConfig["max_questions"]?.toString())
        assertEquals("4", qandaConfig["max_answers"]?.toString())
    }

    @Test
    fun `GET event returns qanda_config when Q&A enabled via public route`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                val event = insertMockedFutureEvent(eventId, orgId = orgId)
                event.qandaEnabled = true
                event.qandaMaxQuestions = 5
                event.qandaMaxAnswers = 3
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val eventObj = body["event"]!!.jsonObject
        val qandaConfig = eventObj["qanda_config"]?.jsonObject
        assertNotNull(qandaConfig)
        assertEquals("5", qandaConfig["max_questions"]?.toString())
        assertEquals("3", qandaConfig["max_answers"]?.toString())
    }
}
