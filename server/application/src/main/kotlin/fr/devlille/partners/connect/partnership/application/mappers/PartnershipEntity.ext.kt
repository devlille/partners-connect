package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.Contact
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity

fun PartnershipEntity.toDomain(emails: List<String>): PartnershipItem = PartnershipItem(
    id = id.toString(),
    contact = Contact(
        displayName = contactName,
        role = contactRole,
    ),
    companyName = company.name,
    eventName = event.name,
    packName = selectedPack?.name,
    suggestedPackName = suggestionPack?.name,
    language = language,
    phone = phone,
    emails = emails,
    createdAt = createdAt,
)
