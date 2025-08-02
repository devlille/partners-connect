package fr.devlille.partners.connect.legaentity.application.mappers

import fr.devlille.partners.connect.legaentity.domain.LegalEntity
import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity

fun LegalEntityEntity.toDomain(): LegalEntity = LegalEntity(
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
