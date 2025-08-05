package fr.devlille.partners.connect.partnership.infrastructure.db

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)

    var eventId by PartnershipsTable.eventId
    var companyId by PartnershipsTable.companyId
    var phone by PartnershipsTable.phone
    var contactName by PartnershipsTable.contactName
    var contactRole by PartnershipsTable.contactRole
    var language by PartnershipsTable.language
    var assignmentUrl by PartnershipsTable.assignmentUrl
    var selectedPackId by PartnershipsTable.selectedPackId
    var suggestionPackId by PartnershipsTable.suggestionPackId
    var suggestionSentAt by PartnershipsTable.suggestionSentAt
    var suggestionApprovedAt by PartnershipsTable.suggestionApprovedAt
    var suggestionDeclinedAt by PartnershipsTable.suggestionDeclinedAt
    var declinedAt by PartnershipsTable.declinedAt
    var validatedAt by PartnershipsTable.validatedAt
    var createdAt by PartnershipsTable.createdAt
}

@Suppress("ReturnCount")
fun PartnershipEntity.validatedPackId(): UUID? {
    if (suggestionPackId != null) {
        if (suggestionApprovedAt.compareToNull(suggestionDeclinedAt) > 0) {
            return suggestionPackId!!
        }
    }
    if (selectedPackId != null) {
        if (validatedAt.compareToNull(declinedAt) > 0) {
            return selectedPackId!!
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

fun UUIDEntityClass<PartnershipEntity>.singleByEventAndCompanyAndPartnership(
    eventId: UUID,
    companyId: UUID,
    partnershipId: UUID,
): PartnershipEntity? = this
    .find {
        (PartnershipsTable.id eq partnershipId) and
            (PartnershipsTable.eventId eq eventId) and
            (PartnershipsTable.companyId eq companyId)
    }
    .singleOrNull()
