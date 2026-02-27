package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable) {
        @Suppress("LongParameterList")
        fun filters(
            eventId: UUID,
            packId: UUID?,
            validated: Boolean?,
            suggestion: Boolean?,
            agreementGenerated: Boolean?,
            agreementSigned: Boolean?,
            organiserUserId: UUID?,
        ): SizedIterable<PartnershipEntity> {
            var op = PartnershipsTable.eventId eq eventId
            if (packId != null) {
                op = op and (PartnershipsTable.selectedPackId eq packId)
            }
            if (validated != null) {
                op = if (validated) {
                    op and (PartnershipsTable.validatedAt.isNotNull())
                } else {
                    op and (PartnershipsTable.validatedAt.isNull())
                }
            }
            if (suggestion != null) {
                op = if (suggestion) {
                    op and (PartnershipsTable.suggestionPackId.isNotNull())
                } else {
                    op and (PartnershipsTable.suggestionPackId.isNull())
                }
            }
            if (agreementGenerated != null) {
                op = if (agreementGenerated) {
                    op and (PartnershipsTable.agreementUrl.isNotNull())
                } else {
                    op and (PartnershipsTable.agreementUrl.isNull())
                }
            }
            if (agreementSigned != null) {
                op = if (agreementSigned) {
                    op and (PartnershipsTable.agreementSignedUrl.isNotNull())
                } else {
                    op and (PartnershipsTable.agreementSignedUrl.isNull())
                }
            }
            if (organiserUserId != null) {
                op = op and (PartnershipsTable.organiserId eq organiserUserId)
            }
            return find { op }
        }

        fun singleByEventAndCompany(eventId: UUID, companyId: UUID): PartnershipEntity? = this
            .find { (PartnershipsTable.eventId eq eventId) and (PartnershipsTable.companyId eq companyId) }
            .singleOrNull()

        fun singleByEventAndPartnership(eventId: UUID, partnershipId: UUID): PartnershipEntity? = this
            .find { (PartnershipsTable.eventId eq eventId) and (PartnershipsTable.id eq partnershipId) }
            .singleOrNull()

        fun singleByCompanyAndPartnership(companyId: UUID, partnershipId: UUID): PartnershipEntity? = this
            .find { (PartnershipsTable.companyId eq companyId) and (PartnershipsTable.id eq partnershipId) }
            .singleOrNull()
    }

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
    var organiser by UserEntity optionalReferencedOn PartnershipsTable.organiserId
    var packPriceOverride by PartnershipsTable.packPriceOverride
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
