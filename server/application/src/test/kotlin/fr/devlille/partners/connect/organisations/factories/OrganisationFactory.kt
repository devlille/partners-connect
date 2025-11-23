package fr.devlille.partners.connect.organisations.factories

import fr.devlille.partners.connect.internal.mockedAdminUser
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import fr.devlille.partners.connect.organisations.domain.Owner
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("LongParameterList")
fun createOrganisation(
    name: String = "DevLille Org",
    headOffice: String? = null,
    siret: String? = null,
    siren: String? = null,
    tva: String? = null,
    dAndB: String? = null,
    nace: String? = null,
    naf: String? = null,
    duns: String? = null,
    iban: String? = null,
    bic: String? = null,
    ribUrl: String? = null,
    representativeUserEmail: String? = null,
    representativeRole: String? = null,
    creationLocation: String? = null,
    createdAt: LocalDateTime? = null,
    publishedAt: LocalDateTime? = null,
): Organisation = Organisation(
    name = name,
    headOffice = headOffice,
    siret = siret,
    siren = siren,
    tva = tva,
    dAndB = dAndB,
    nace = nace,
    naf = naf,
    duns = duns,
    iban = iban,
    bic = bic,
    ribUrl = ribUrl,
    representativeUserEmail = representativeUserEmail,
    representativeRole = representativeRole,
    creationLocation = creationLocation,
    createdAt = createdAt?.date,
    publishedAt = publishedAt?.date,
)

// Convenience function for creating a fully populated organisation for tests
fun createFullOrganisation(
    name: String = "DevLille Org",
): Organisation = createOrganisation(
    name = name,
    headOffice = "123 rue de la République, Lille, France",
    siret = "12345678900019",
    siren = "123456789",
    tva = "FR123456789",
    dAndB = "123456789",
    nace = "62.01Z",
    naf = "62.01Z",
    duns = "987654321",
    iban = "FR7630006000011234567890189",
    bic = "AGRIFRPPXXX",
    ribUrl = "https://example.com/rib.pdf",
    representativeUserEmail = mockedAdminUser.email,
    representativeRole = "President",
    creationLocation = "Lille",
    createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    publishedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
)

fun createOrganisationItem(
    name: String = "DevLille Org",
    slug: String = "devlille-org",
    headOffice: String = "123 rue de la République, Lille, France",
    displayName: String = "Test User",
    email: String = mockedAdminUser.email,
): OrganisationItem = OrganisationItem(
    name = name,
    slug = slug,
    headOffice = headOffice,
    owner = Owner(
        displayName = displayName,
        email = email,
    ),
)
