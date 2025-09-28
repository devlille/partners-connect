package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsoringDeleteRoutesTest {
    @Test
    fun `DELETE option from pack - success`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-1"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringPack(packId, eventId, maxQuantity = null)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedPackOptions(packId, optionId)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/packs/$packId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE option from pack - not attached`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/packs/$packId/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE option - used in pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-3"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            val pack = insertMockedSponsoringPack(event = eventId)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
            insertMockedPackOptions(pack.id.value, optionId)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun `DELETE option - not used in any pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-4"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringOption(optionId = optionId, eventId = eventId)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE pack - fails if has options`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-event-slug-5"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringPack(packId, eventId, maxQuantity = null)
            val option = insertMockedSponsoringOption(eventId = eventId)
            insertMockedPackOptions(packId, option.id.value)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("cannot be deleted"))
    }

    @Test
    fun `DELETE pack - success when no options`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-event-slug-6"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringPack(packId, eventId, maxQuantity = null)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE option - not found`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-7"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found", ignoreCase = true))
    }

    @Test
    fun `DELETE pack - not found`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val eventSlug = "test-event-slug-8"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Pack not found", ignoreCase = true))
    }
}
