package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnershipSuggestionRoutesTest {
    @Test
    fun `PUT suggests a new pack with optional options`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
            )
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = option
                    this.language = "en"
                    this.name = "Logo"
                    this.description = "Company logo"
                }
                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                Json.encodeToString(
                    SuggestPartnership(packId.toString(), listOf(optionId.toString()), "en"),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        val suggestionPack = transaction { partnership?.suggestionPack }
        assertEquals(packId, suggestionPack?.id?.value)
        assertNull(partnership?.suggestionApprovedAt)
        assertNull(partnership?.suggestionDeclinedAt)
    }

    @Test
    fun `PUT fails if partnership does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val fakeId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$fakeId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership("fake-pack", emptyList(), "en")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if pack does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(UUID.randomUUID().toString(), emptyList(), "en")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if option is not optional in pack`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
            )
            insertMockSponsoringPack(packId, eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                OptionTranslationEntity.new {
                    this.option = option
                    this.language = "en"
                    this.name = "Logo"
                    this.description = "Company logo"
                }

                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = true
                }
            }
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(packId.toString(), listOf(optionId.toString()), "en")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `PUT fails if option translation missing`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
                language = "fr",
            )
            insertMockSponsoringPack(packId, eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }

                PackOptionsTable.insert {
                    it[this.pack] = packId
                    it[this.option] = optionId
                    it[this.required] = false
                }
            }
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(packId.toString(), listOf(optionId.toString()), "en")))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("does not have a translation"))
    }
}
