package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartnershipSuggestionRoutesTest {
    @Test
    fun `PUT suggests a new pack with optional options`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-1"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedOptionTranslation(optionId = optionId)
            insertMockedPackOptions(packId, optionId, required = false)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/suggestion") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val fakeId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$fakeId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership("fake-pack", emptyList(), "en")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if pack does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-slug-3"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(UUID.randomUUID().toString(), emptyList(), "en")))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails if option is not optional in pack`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-4"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedOptionTranslation(optionId = optionId)
            insertMockedPackOptions(packId, optionId)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(packId.toString(), listOf(optionId.toString()), "en")))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("not optional"))
    }

    @Test
    fun `PUT fails if option translation missing`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-5"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId, language = "fr")
            insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedPackOptions(packId, optionId, required = false)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/suggestion") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(SuggestPartnership(packId.toString(), listOf(optionId.toString()), "en")))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("doesn't have a translation"))
    }
}
