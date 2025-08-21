package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)

    var event by EventEntity referencedOn PartnershipsTable.eventId
    var company by CompanyEntity referencedOn PartnershipsTable.companyId
    var phone by PartnershipsTable.phone
    var contactName by PartnershipsTable.contactName
    var contactRole by PartnershipsTable.contactRole
    var language by PartnershipsTable.language
    var agreementUrl by PartnershipsTable.agreementUrl
    var agreementSignedUrl by PartnershipsTable.agreementSignedUrl
    var selectedPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.selectedPackId
    var suggestionPack by SponsoringPackEntity optionalReferencedOn PartnershipsTable.suggestionPackId
    var suggestionSentAt by PartnershipsTable.suggestionSentAt
    var suggestionApprovedAt by PartnershipsTable.suggestionApprovedAt
    var suggestionDeclinedAt by PartnershipsTable.suggestionDeclinedAt
    var declinedAt by PartnershipsTable.declinedAt
    var validatedAt by PartnershipsTable.validatedAt
    var boothLocation by PartnershipsTable.boothLocation
    var communicationPublicationDate by PartnershipsTable.communicationPublicationDate
    var communicationSupportUrl by PartnershipsTable.communicationSupportUrl
    var createdAt by PartnershipsTable.createdAt
}

@Suppress("ReturnCount")
fun PartnershipEntity.validatedPack(): SponsoringPackEntity? {
    if (suggestionPack != null) {
        if (suggestionApprovedAt.compareToNull(suggestionDeclinedAt) > 0) {
            return suggestionPack
        }
    }
    if (selectedPack != null) {
        if (validatedAt.compareToNull(declinedAt) > 0) {
            return selectedPack
        }
    }
    return null
}

private fun LocalDateTime?.compareToNull(other: LocalDateTime?): Int {
    return if (this == null && other == null) {
        0
    } else if (this == null) {
        -1
    } else if (other == null) {
        1
    } else {
        this.compareTo(other)
    }
}

fun UUIDEntityClass<PartnershipEntity>.singleByEventAndCompany(
    eventId: UUID,
    companyId: UUID,
): PartnershipEntity? = this
    .find { (PartnershipsTable.eventId eq eventId) and (PartnershipsTable.companyId eq companyId) }
    .singleOrNull()

fun UUIDEntityClass<PartnershipEntity>.singleByEventAndPartnership(
    eventId: UUID,
    partnershipId: UUID,
): PartnershipEntity? = this
    .find { (PartnershipsTable.eventId eq eventId) and (PartnershipsTable.id eq partnershipId) }
    .singleOrNull()
