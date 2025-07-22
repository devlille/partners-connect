package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class PartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEntity>(PartnershipsTable)

    var eventId by PartnershipsTable.eventId
    var companyId by PartnershipsTable.companyId
    var phone by PartnershipsTable.phone
    var language by PartnershipsTable.language
    var selectedPackId by PartnershipsTable.selectedPackId
    var suggestionPackId by PartnershipsTable.suggestionPackId
    var suggestionSentAt by PartnershipsTable.suggestionSentAt
    var suggestionApprovedAt by PartnershipsTable.suggestionApprovedAt
    var suggestionDeclinedAt by PartnershipsTable.suggestionDeclinedAt
    var validatedAt by PartnershipsTable.validatedAt
    var createdAt by PartnershipsTable.createdAt
}
