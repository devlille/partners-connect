package fr.devlille.partners.connect.organisations.factories

import fr.devlille.partners.connect.organisations.domain.Organisation
import kotlinx.datetime.LocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun createOrganisation(
    name: String = UUID.randomUUID().toString(),
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
