package fr.devlille.partners.connect.companies.application.mappers

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.domain.Social
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity

internal fun CompanyEntity.toDomain(
    socials: List<Social>,
): Company {
    val hasMedia =
        logoUrlOriginal != null && logoUrl1000 != null && logoUrl500 != null && logoUrl250 != null
    return Company(
        id = id.value.toString(),
        name = name,
        headOffice = Address(
            address = address,
            city = city,
            zipCode = zipCode,
            country = country,
        ),
        siret = siret,
        vat = vat,
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
        status = status,
        socials = socials,
    )
}

internal fun CompanySocialEntity.toDomain(): Social = Social(type = type, url = url)
