package fr.devlille.partners.connect.organisations.application.mappers

import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import fr.devlille.partners.connect.organisations.domain.Owner
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity

fun OrganisationEntity.toDomain(): OrganisationItem = OrganisationItem(
    name = this.name,
    slug = this.slug,
    headOffice = this.headOffice,
    owner = Owner(
        displayName = this.representativeUser.name ?: "",
        email = this.representativeUser.email,
    ),
)
