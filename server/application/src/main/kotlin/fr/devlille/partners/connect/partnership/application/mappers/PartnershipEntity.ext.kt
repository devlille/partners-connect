package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.Contact
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import fr.devlille.partners.connect.partnership.domain.PartnershipProcessStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack

fun PartnershipEntity.toDomain(emails: List<String>): PartnershipItem = PartnershipItem(
    id = id.toString(),
    contact = Contact(
        displayName = contactName,
        role = contactRole,
    ),
    companyName = company.name,
    eventName = event.name,
    selectedPackId = selectedPack?.id?.value?.toString(),
    selectedPackName = selectedPack?.name,
    suggestedPackId = suggestionPack?.id?.value?.toString(),
    suggestedPackName = suggestionPack?.name,
    validatedPackId = validatedPack()?.id?.value?.toString(),
    language = language,
    phone = phone,
    emails = emails,
    createdAt = createdAt,
)

fun PartnershipEntity.toDetailedDomain(
    billing: BillingEntity?,
    selectedPack: PartnershipPack?,
    suggestionPack: PartnershipPack?,
    validatedPack: PartnershipPack?,
): PartnershipDetail = PartnershipDetail(
    id = id.toString(),
    phone = phone,
    contactName = contactName,
    contactRole = contactRole,
    language = language,
    emails = PartnershipEmailEntity.emails(id.value),
    selectedPack = selectedPack,
    suggestionPack = suggestionPack,
    validatedPack = validatedPack,
    processStatus = toProcessStatus(billing),
    createdAt = createdAt.toString(),
)

fun PartnershipEntity.toProcessStatus(billing: BillingEntity?): PartnershipProcessStatus = PartnershipProcessStatus(
    suggestionSentAt = suggestionSentAt?.toString(),
    suggestionApprovedAt = suggestionApprovedAt?.toString(),
    suggestionDeclinedAt = suggestionDeclinedAt?.toString(),
    validatedAt = validatedAt?.toString(),
    declinedAt = declinedAt?.toString(),
    agreementUrl = agreementUrl,
    agreementSignedUrl = agreementSignedUrl,
    communicationPublicationDate = communicationPublicationDate?.toString(),
    communicationSupportUrl = communicationSupportUrl,
    billingStatus = billing?.status?.name,
)
