package fr.devlille.partners.connect.digest.application

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedFlyerEnabledPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigestFlyerEligibilityTest {
    @Test
    fun `digest flyerItems lists validated partnerships on flyer-enabled packs without a flyer yet`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "event-${UUID.randomUUID()}"
        val flyerPackId = UUID.randomUUID()
        val plainPackId = UUID.randomUUID()
        val eligibleCompanyId = UUID.randomUUID()
        val alreadyGeneratedCompanyId = UUID.randomUUID()
        val plainPackCompanyId = UUID.randomUUID()
        val notValidatedCompanyId = UUID.randomUUID()
        val eligiblePartnershipId = UUID.randomUUID()
        val alreadyGeneratedPartnershipId = UUID.randomUUID()
        val plainPackPartnershipId = UUID.randomUUID()
        val notValidatedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, slug = eventSlug, orgId = orgId)
                insertMockedCompany(eligibleCompanyId)
                insertMockedCompany(alreadyGeneratedCompanyId)
                insertMockedCompany(plainPackCompanyId)
                insertMockedCompany(notValidatedCompanyId)
                insertMockedFlyerEnabledPack(packId = flyerPackId, eventId = eventId)
                insertMockedSponsoringPack(id = plainPackId, eventId = eventId)
                insertMockedValidatedPartnership(
                    id = eligiblePartnershipId,
                    eventId = eventId,
                    companyId = eligibleCompanyId,
                    selectedPackId = flyerPackId,
                )
                insertMockedValidatedPartnership(
                    id = alreadyGeneratedPartnershipId,
                    eventId = eventId,
                    companyId = alreadyGeneratedCompanyId,
                    selectedPackId = flyerPackId,
                ).apply { communicationSupportUrl = "https://example.com/flyer.jpg" }
                insertMockedValidatedPartnership(
                    id = plainPackPartnershipId,
                    eventId = eventId,
                    companyId = plainPackCompanyId,
                    selectedPackId = plainPackId,
                )
                insertMockedPartnership(
                    id = notValidatedPartnershipId,
                    eventId = eventId,
                    companyId = notValidatedCompanyId,
                    selectedPackId = flyerPackId,
                )
            }
        }
        // Force application initialisation (testApplication is lazy) by issuing any request.
        client.get("/")

        val digest = runBlocking {
            DigestRepositoryExposed().queryDigest(eventSlug, LocalDate(2026, 5, 16))
        }

        assertEquals(1, digest.flyerItems.size, "Only the eligible partnership should be listed")
        assertTrue(
            digest.flyerItems.first().companyName == eligibleCompanyId.toString(),
            "Expected eligible company in flyerItems, got: ${digest.flyerItems}",
        )
    }
}
