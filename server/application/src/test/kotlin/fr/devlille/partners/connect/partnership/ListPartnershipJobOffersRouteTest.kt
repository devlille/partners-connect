package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for GET /events/{eventSlug}/partnerships/{partnershipId}/job-offers endpoint.
 * Tests retrieving promoted job offers for a specific partnership.
 */
class ListPartnershipJobOffersRouteTest {
    @Test
    fun `GET partnership job offers returns 200 with empty list when no promotions exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
                insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.get("/events/$eventSlug/partnerships/$partnershipId/job-offers")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"items\":[]") || body.contains("[]"))
    }

    @Test
    fun `GET partnership job offers returns 200 with paginated results`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val eventSlug = "devconf-2025"
        val orgSlug = "devconf"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity(name = orgSlug)
                insertMockedEventWithOrga(
                    id = eventId,
                    slug = eventSlug,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedOrgaPermission(orgId = org.id.value, user = insertMockedAdminUser())
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = jobOfferId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    id = promotionId,
                )
            }
        }

        val response = client.get(
            "/events/$eventSlug/partnerships/$partnershipId/job-offers?page=1&page_size=10",
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.isNotEmpty())
    }

    @Test
    fun `GET partnership job offers returns 404 when partnership does not exist`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val nonExistentPartnershipId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
                insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            }
        }

        val response = client.get(
            "/events/$eventSlug/partnerships/$nonExistentPartnershipId/job-offers",
        )

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET partnership job offers returns 404 when event does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
            }
        }

        val response = client.get(
            "/events/non-existent-event/partnerships/$partnershipId/job-offers",
        )

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
