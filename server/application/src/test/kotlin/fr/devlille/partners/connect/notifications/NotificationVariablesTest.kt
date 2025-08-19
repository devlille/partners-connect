package fr.devlille.partners.connect.notifications

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationVariablesTest {
    private lateinit var event: Event
    private lateinit var eventWithOrganisation: EventWithOrganisation
    private lateinit var company: Company
    private lateinit var pack: PartnershipPack
    private lateinit var partnershipId: UUID

    @BeforeTest
    fun setUp() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        event = Event(
            name = "Test Event",
            startTime = now,
            endTime = now,
            submissionStartTime = now,
            submissionEndTime = now,
            address = "Test Address",
            contact = Contact(email = "test@example.com", phone = null),
        )

        val organisation = Organisation(
            name = "Test Org",
            slug = "test-org",
            headOffice = "Test HQ",
            siret = null,
            siren = null,
            tva = null,
            dAndB = null,
            nace = null,
            naf = null,
            duns = null,
            iban = "FR123456789",
            bic = "TESTFRPPXXX",
            ribUrl = "https://example.com/rib",
            representativeUserEmail = "rep@example.com",
            representativeRole = "President",
            creationLocation = "Paris",
            createdAt = now,
            publishedAt = now,
        )

        eventWithOrganisation = EventWithOrganisation(event = event, organisation = organisation)

        company = Company(
            id = "test-company-id",
            name = "Test Company",
            headOffice = Address(
                address = "123 Test St",
                city = "Test City",
                zipCode = "12345",
                country = "FR",
            ),
            siret = "12345678901234",
            vat = "FR12345678901",
            description = "Test company",
            siteUrl = "https://testcompany.com",
            medias = null,
        )

        pack = PartnershipPack(
            id = "test-pack-id",
            name = "Test Pack",
            basePrice = 1000,
            options = emptyList(),
        )

        partnershipId = UUID.randomUUID()
    }

    @Test
    fun `NewPartnership should include partnership link in populated content`() {
        val eventSlug = "test-event"
        val partnershipContext = NotificationVariables.PartnershipContext(
            eventWithOrganisation,
            eventSlug,
            partnershipId,
        )

        val variables = NotificationVariables.NewPartnership(
            language = "en",
            event = event,
            company = company,
            pack = pack,
            partnershipContext = partnershipContext,
        )

        val content = "Test content with {{partnership_link}} placeholder."
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-org/$eventSlug/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content. Got: $populated")
        assertTrue(!populated.contains("{{partnership_link}}"), "Placeholder should be replaced")
    }

    @Test
    fun `PartnershipValidated should include partnership link in populated content`() {
        val eventSlug = "test-event"
        val partnershipContext = NotificationVariables.PartnershipContext(
            eventWithOrganisation,
            eventSlug,
            partnershipId,
        )

        val variables = NotificationVariables.PartnershipValidated(
            language = "en",
            event = event,
            company = company,
            pack = pack,
            partnershipContext = partnershipContext,
        )

        val content = "Partnership validated. View: {{partnership_link}}"
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-org/$eventSlug/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content")
    }

    @Test
    fun `SuggestionApproved should include partnership link in populated content`() {
        val eventSlug = "test-event"
        val partnershipContext = NotificationVariables.PartnershipContext(
            eventWithOrganisation,
            eventSlug,
            partnershipId,
        )

        val variables = NotificationVariables.SuggestionApproved(
            language = "en",
            event = event,
            company = company,
            partnershipContext = partnershipContext,
        )

        val content = "Suggestion approved! Check: {{partnership_link}}"
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-org/$eventSlug/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content")
    }
}
