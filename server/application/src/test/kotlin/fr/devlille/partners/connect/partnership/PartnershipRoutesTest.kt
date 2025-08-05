package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipRoutesTest {
    @Test
    fun `POST registers a valid partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = SponsoringOptionEntity[optionId]
                    this.language = "en"
                    this.name = "Logo"
                    this.description = "Display logo"
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val body = RegisterPartnership(
            packId = packId.toString(),
            optionIds = listOf(optionId.toString()),
            language = "en",
            phone = "+33600000000",
            emails = listOf("partner@example.com"),
        )

        val response = client.post("/events/$eventId/companies/$companyId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), body))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("id"))
    }

    @Test
    fun `POST returns 404 when event not found`() = testApplication {
        val response = client.post("/events/${UUID.randomUUID()}/companies/${UUID.randomUUID()}/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), RegisterPartnership("pack", listOf(), "en")))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company not found`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.post("/events/$eventId/companies/${UUID.randomUUID()}/partnership") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterPartnership.serializer(), RegisterPartnership("pack", listOf(), "en")))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when pack not found`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEvent(eventId)
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterPartnership.serializer(),
                    RegisterPartnership(UUID.randomUUID().toString(), listOf(), "en"),
                ),
            )
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 400 when partnership already exists`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            insertMockPartnership(eventId, companyId, selectedPackId = packId)
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterPartnership.serializer(),
                    RegisterPartnership(packId.toString(), listOf(), "en"),
                ),
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 400 when option not optional`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 200
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = true
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterPartnership.serializer(),
                    RegisterPartnership(packId.toString(), listOf(optionId.toString()), "en"),
                ),
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `POST returns 400 when option has no translation`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockCompany(companyId)
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 500
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterPartnership.serializer(),
                    RegisterPartnership(packId.toString(), listOf(optionId.toString()), "fr"),
                ),
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }
}
