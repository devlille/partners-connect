package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockPartnership(
    eventId: UUID = UUID.randomUUID(),
    companyId: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    phone: String? = null,
    language: String = "en",
    assignmentUrl: String? = null,
    selectedPackId: UUID? = null,
    suggestionPackId: UUID? = null,
    suggestionSentAt: LocalDateTime? = null,
    suggestionApprovedAt: LocalDateTime? = null,
    suggestionDeclinedAt: LocalDateTime? = null,
    declinedAt: LocalDateTime? = null,
    validatedAt: LocalDateTime? = null,
): PartnershipEntity = transaction {
    PartnershipEntity.new(partnershipId) {
        this.eventId = eventId
        this.companyId = companyId
        this.phone = phone
        this.language = language
        this.assignmentUrl = assignmentUrl
        this.selectedPackId = selectedPackId
        this.suggestionPackId = suggestionPackId
        this.suggestionSentAt = suggestionSentAt
        this.suggestionApprovedAt = suggestionApprovedAt
        this.suggestionDeclinedAt = suggestionDeclinedAt
        this.declinedAt = declinedAt
        this.validatedAt = validatedAt
    }
}
