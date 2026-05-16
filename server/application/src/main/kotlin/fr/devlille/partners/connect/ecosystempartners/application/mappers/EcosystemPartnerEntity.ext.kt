package fr.devlille.partners.connect.ecosystempartners.application.mappers

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerItem
import fr.devlille.partners.connect.ecosystempartners.domain.PublicEcosystemPartner
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity

fun EcosystemPartnerEntity.toDomain(emails: List<String>): EcosystemPartner = EcosystemPartner(
    id = id.value.toString(),
    eventId = event.id.value.toString(),
    companyId = company.id.value.toString(),
    category = category.toDomain(),
    displayOrder = displayOrder,
    language = language,
    emails = emails,
    validatedAt = validatedAt?.toString(),
    declinedAt = declinedAt?.toString(),
    createdAt = createdAt.toString(),
)

fun EcosystemPartnerEntity.toItem(): EcosystemPartnerItem = EcosystemPartnerItem(
    id = id.value.toString(),
    companyName = company.name,
    companyLogoUrl = company.logoUrl500 ?: company.logoUrl1000 ?: company.logoUrlOriginal,
    category = category.toDomain(),
    displayOrder = displayOrder,
    validatedAt = validatedAt,
    declinedAt = declinedAt,
    createdAt = createdAt,
)

fun EcosystemPartnerEntity.toPublic(): PublicEcosystemPartner = PublicEcosystemPartner(
    id = id.value.toString(),
    companyName = company.name,
    logoUrl = company.logoUrl500 ?: company.logoUrl1000 ?: company.logoUrlOriginal,
    siteUrl = company.siteUrl,
)
