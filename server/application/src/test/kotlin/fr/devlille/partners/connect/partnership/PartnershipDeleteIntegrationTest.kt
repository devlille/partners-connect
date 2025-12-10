package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipEmail
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipTicket
import fr.devlille.partners.connect.partnership.factories.insertMockedSpeakerPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipTable
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId} endpoint.
 * Tests end-to-end business logic including database operations, authorization, and state validation.
 */
class PartnershipDeleteIntegrationTest {
    @Test
    fun `deleted partnership no longer appears in list`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(
                eventId = eventId,
                orgId = orgId,
                slug = eventSlug,
            )
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = null,
                declinedAt = null,
            )
        }

        // First verify partnership exists in list
        val listBeforeResponse = client.get("/orgs/$orgId/events/$eventSlug/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.OK, listBeforeResponse.status)

        // Delete partnership
        val deleteResponse = client.delete("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify partnership no longer in list
        val listAfterResponse = client.get("/orgs/$orgId/events/$eventSlug/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.OK, listAfterResponse.status)

        // Verify GET by ID returns 404
        val getByIdResponse = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.NotFound, getByIdResponse.status)
    }

    @Test
    @Suppress("LongMethod")
    fun `deleted partnership cascades to emails, options, tickets, speakers, and billing records`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val speakerId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(
                eventId = eventId,
                orgId = orgId,
                slug = eventSlug,
            )
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedSponsoringOption(optionId, eventId)
            insertMockedSpeaker(id = speakerId, eventId = eventId)

            // Create partnership with emails and options
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = null,
                declinedAt = null,
            )

            // Add partnership emails
            insertMockedPartnershipEmail(partnershipId, "contact1@example.com")
            insertMockedPartnershipEmail(partnershipId, "contact2@example.com")

            // Add partnership options
            insertMockedOptionPartnership(partnershipId, packId, optionId)

            // Add partnership tickets
            insertMockedPartnershipTicket(partnershipId = partnershipId)
            insertMockedPartnershipTicket(partnershipId = partnershipId)

            // Add speaker partnership
            insertMockedSpeakerPartnership(partnershipId = partnershipId, speakerId = speakerId)

            // Verify related records exist before deletion
            transaction {
                val emailsBeforeDelete = PartnershipEmailEntity
                    .find { PartnershipEmailsTable.partnershipId eq partnershipId }
                    .count()
                assertEquals(2, emailsBeforeDelete, "Should have 2 emails before delete")

                val optionsBeforeDelete = PartnershipOptionEntity
                    .find { PartnershipOptionsTable.partnershipId eq partnershipId }
                    .count()
                assertEquals(1, optionsBeforeDelete, "Should have 1 option before delete")

                val ticketsBeforeDelete = PartnershipTicketEntity
                    .find { PartnershipTicketsTable.partnershipId eq partnershipId }
                    .count()
                assertEquals(2, ticketsBeforeDelete, "Should have 2 tickets before delete")

                val speakersBeforeDelete = SpeakerPartnershipEntity
                    .find { SpeakerPartnershipTable.partnershipId eq partnershipId }
                    .count()
                assertEquals(1, speakersBeforeDelete, "Should have 1 speaker partnership before delete")
            }
        }

        // Delete partnership
        val deleteResponse = client.delete("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify all related records are deleted
        transaction {
            val emailsAfterDelete = PartnershipEmailEntity
                .find { PartnershipEmailsTable.partnershipId eq partnershipId }
                .count()
            assertEquals(0, emailsAfterDelete, "All emails should be deleted")

            val optionsAfterDelete = PartnershipOptionEntity
                .find { PartnershipOptionsTable.partnershipId eq partnershipId }
                .count()
            assertEquals(0, optionsAfterDelete, "All options should be deleted")

            val ticketsAfterDelete = PartnershipTicketEntity
                .find { PartnershipTicketsTable.partnershipId eq partnershipId }
                .count()
            assertEquals(0, ticketsAfterDelete, "All tickets should be deleted")

            val speakersAfterDelete = SpeakerPartnershipEntity
                .find { SpeakerPartnershipTable.partnershipId eq partnershipId }
                .count()
            assertEquals(0, speakersAfterDelete, "All speaker partnerships should be deleted")

            val billingsAfterDelete = BillingEntity
                .find { BillingsTable.partnershipId eq partnershipId }
                .count()
            assertEquals(0, billingsAfterDelete, "All billing records should be deleted")
        }
    }
}
