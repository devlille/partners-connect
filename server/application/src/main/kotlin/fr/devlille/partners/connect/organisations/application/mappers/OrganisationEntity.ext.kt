package fr.devlille.partners.connect.organisations.application.mappers

import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity

fun OrganisationEntity.toDomain(): Organisation = Organisation(
    name = this.name,
    headOffice = this.headOffice,
    siret = this.siret,
    siren = this.siren,
    tva = this.tva,
    dAndB = this.dAndB,
    nace = this.nace,
    naf = this.naf,
    duns = this.duns,
    iban = this.iban,
    bic = this.bic,
    ribUrl = this.ribUrl,
    creationLocation = this.creationLocation,
    createdAt = this.createdAt,
    publishedAt = this.publishedAt,
    representativeUserEmail = this.representativeUser.email,
    representativeRole = this.representativeRole,
)
