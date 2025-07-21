package fr.devlille.partners.connect.companies.application.mappers

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity

internal fun CompanyEntity.toDomain(): Company {
    val hasMedia =
        logoUrlOriginal != null && logoUrl1000 != null && logoUrl500 != null && logoUrl250 != null
    return Company(
        id = id.value.toString(),
        name = name,
        description = description,
        siteUrl = siteUrl,
        medias = if (hasMedia) {
            Media(
                original = logoUrlOriginal!!,
                png1000 = logoUrl1000!!,
                png500 = logoUrl500!!,
                png250 = logoUrl250!!,
            )
        } else {
            null
        },
    )
}
