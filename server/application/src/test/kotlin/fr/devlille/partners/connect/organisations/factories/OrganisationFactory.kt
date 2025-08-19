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
    headOffice: String = "123 rue de la République, Lille, France",
    siret: String = "12345678900019",
    siren: String = "123456789",
    tva: String = "FR123456789",
    dAndB: String = "123456789",
    nace: String = "62.01Z",
    naf: String = "62.01Z",
    duns: String = "987654321",
    iban: String = "FR7630006000011234567890189",
    bic: String = "AGRIFRPPXXX",
    ribUrl: String = "https://example.com/rib.pdf",
    representativeUserEmail: String = mockedAdminUser.email,
    representativeRole: String = "President",
    creationLocation: String = "Lille",
    createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    publishedAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
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
    createdAt = createdAt,
    publishedAt = publishedAt,
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
