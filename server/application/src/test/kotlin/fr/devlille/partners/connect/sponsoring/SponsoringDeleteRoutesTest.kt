package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.insertMockedAdminUser
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsoringDeleteRoutesTest {
    @Test
    fun `DELETE option from pack - success`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 100
                }
                val pack = SponsoringPackEntity.new(packId) {
                    this.eventId = eventId
                    this.name = "Pack A"
                    this.basePrice = 100
                    this.maxQuantity = null
                }
                PackOptionsTable.insert {
                    it[this.pack] = pack.id
                    it[this.option] = option.id
                    it[this.required] = true
                }
            }
        }

        val response = client.delete("/events/$eventId/packs/$packId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE option from pack - not attached`() = testApplication {
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        val response = client.delete("/events/$eventId/packs/$packId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE option - used in pack`() = testApplication {
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
            transaction {
                val option = SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 50
                }
                val pack = SponsoringPackEntity.new {
                    this.eventId = eventId
                    this.name = "Gold"
                    this.basePrice = 300
                    this.maxQuantity = null
                }
                PackOptionsTable.insert {
                    it[this.pack] = pack.id
                    it[this.option] = option.id
                    it[this.required] = false
                }
            }
        }

        val response = client.delete("/events/$eventId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun `DELETE option - not used in any pack`() = testApplication {
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
            transaction {
                SponsoringOptionEntity.new(optionId) {
                    this.eventId = eventId
                    this.price = 80
                }
            }
        }

        val response = client.delete("/events/$eventId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE pack - fails if has options`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
            transaction {
                val option = SponsoringOptionEntity.new {
                    this.eventId = eventId
                    this.price = 60
                }
                val pack = SponsoringPackEntity.new(packId) {
                    this.eventId = eventId
                    this.name = "Silver"
                    this.basePrice = 200
                    this.maxQuantity = null
                }
                PackOptionsTable.insert {
                    it[this.pack] = pack.id
                    it[this.option] = option.id
                    it[this.required] = true
                }
            }
        }

        val response = client.delete("/events/$eventId/packs/$packId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun `DELETE pack - success when no options`() = testApplication {
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
            transaction {
                SponsoringPackEntity.new(packId) {
                    this.eventId = eventId
                    this.name = "Bronze"
                    this.basePrice = 100
                    this.maxQuantity = null
                }
            }
        }

        val response = client.delete("/events/$eventId/packs/$packId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
}
