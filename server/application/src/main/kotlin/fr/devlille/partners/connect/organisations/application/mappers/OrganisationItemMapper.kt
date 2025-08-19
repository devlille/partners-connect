package fr.devlille.partners.connect.organisations.application.mappers

import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import fr.devlille.partners.connect.organisations.domain.Owner
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity

fun OrganisationEntity.toItemDomain(): OrganisationItem = OrganisationItem(
    name = this.name,
    slug = this.slug,
    headOffice = this.headOffice ?: "",
    owner = this.representativeUser?.let { user ->
        Owner(
            displayName = user.name ?: "",
            email = user.email,
        )
    },
)
