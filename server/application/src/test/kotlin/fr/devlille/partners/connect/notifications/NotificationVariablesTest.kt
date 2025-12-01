package fr.devlille.partners.connect.notifications

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventDisplay
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import fr.devlille.partners.connect.organisations.domain.Owner
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationVariablesTest {
    private lateinit var event: Event
    private lateinit var eventDisplay: EventDisplay
    private lateinit var eventWithOrganisation: EventWithOrganisation
    private lateinit var company: Company
    private lateinit var pack: PartnershipPack
    private lateinit var partnership: Partnership
    private lateinit var partnershipId: String

    @BeforeTest
    fun setUp() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        event = createEvent(now)
        eventDisplay = createEventDisplay(now)
        val organisationItem = createOrganisationItem()
        eventWithOrganisation = EventWithOrganisation(
            event = eventDisplay,
            organisation = organisationItem,
        )
        company = createCompany()
        pack = createPack()
        partnershipId = "test-partnership-123"
        partnership = createPartnership()
    }

    private fun createEvent(now: LocalDateTime) = Event(
        name = "Test Event",
        startTime = now,
        endTime = now,
        submissionStartTime = now,
        submissionEndTime = now,
        address = "Test Address",
        contact = Contact(email = "test@example.com", phone = null),
    )

    private fun createEventDisplay(now: LocalDateTime) = EventDisplay(
        id = "test-event-123",
        slug = "test-event",
        name = "Test Event",
        startTime = now,
        endTime = now,
        submissionStartTime = now,
        submissionEndTime = now,
        address = "Test Address",
        contact = Contact(email = "test@example.com", phone = null),
        externalLinks = emptyList(),
        providers = emptyList(),
    )

    private fun createOrganisationItem() = OrganisationItem(
        name = "Test Org",
        slug = "test-org",
        headOffice = "Test HQ",
        owner = Owner(
            displayName = "Test User",
            email = "test@example.com",
        ),
    )

    private fun createCompany() = Company(
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
        status = CompanyStatus.ACTIVE,
        socials = emptyList(),
    )

    private fun createPack() = PartnershipPack(
        id = "test-pack-id",
        name = "Test Pack",
        basePrice = 1000,
        requiredOptions = emptyList(),
        optionalOptions = emptyList(),
        totalPrice = 1000,
    )

    private fun createPartnership() = Partnership(
        id = partnershipId,
        phone = null,
        emails = emptyList(),
        language = "en",
        selectedPack = pack,
        suggestionPack = null,
    )

    @Test
    fun `NewPartnership should include partnership link in populated content`() {
        val variables = NotificationVariables.NewPartnership(
            language = "en",
            event = eventWithOrganisation,
            company = company,
            partnership = partnership,
            pack = pack,
        )

        val content = "Test content with {{partnership_link}} placeholder."
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-event/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content. Got: $populated")
        assertTrue(!populated.contains("{{partnership_link}}"), "Placeholder should be replaced")
    }

    @Test
    fun `PartnershipValidated should include partnership link in populated content`() {
        val variables = NotificationVariables.PartnershipValidated(
            language = "en",
            event = eventWithOrganisation,
            company = company,
            partnership = partnership,
            pack = pack,
        )

        val content = "Partnership validated. View: {{partnership_link}}"
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-event/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content")
    }

    @Test
    fun `SuggestionApproved should include partnership link in populated content`() {
        val variables = NotificationVariables.SuggestionApproved(
            language = "en",
            event = eventWithOrganisation,
            company = company,
            partnership = partnership,
        )

        val content = "Suggestion approved! Check: {{partnership_link}}"
        val populated = variables.populate(content)

        val expectedLink = "${SystemVarEnv.frontendBaseUrl}/test-event/$partnershipId"
        assertTrue(populated.contains(expectedLink), "Partnership link should be replaced in content")
    }
}
