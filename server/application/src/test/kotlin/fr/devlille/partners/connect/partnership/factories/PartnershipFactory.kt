package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedPartnership(
    id: UUID = UUID.randomUUID(),
    eventId: UUID = UUID.randomUUID(),
    companyId: UUID = UUID.randomUUID(),
    phone: String? = null,
    contactName: String = "John Doe",
    contactRole: String = "Developer",
    language: String = "en",
    agreementUrl: String? = null,
    agreementSignedUrl: String? = null,
    selectedPackId: UUID? = null,
    suggestionPackId: UUID? = null,
    suggestionSentAt: LocalDateTime? = null,
    suggestionApprovedAt: LocalDateTime? = null,
    suggestionDeclinedAt: LocalDateTime? = null,
    declinedAt: LocalDateTime? = null,
    validatedAt: LocalDateTime? = null,
): PartnershipEntity = transaction {
    PartnershipEntity.new(id) {
        this.event = EventEntity[eventId]
        this.company = CompanyEntity[companyId]
        this.phone = phone
        this.contactName = contactName
        this.contactRole = contactRole
        this.language = language
        this.agreementUrl = agreementUrl
        this.agreementSignedUrl = agreementSignedUrl
        this.selectedPack = selectedPackId?.let { SponsoringPackEntity[selectedPackId] }
        this.suggestionPack = suggestionPackId?.let { SponsoringPackEntity[suggestionPackId] }
        this.suggestionSentAt = suggestionSentAt
        this.suggestionApprovedAt = suggestionApprovedAt
        this.suggestionDeclinedAt = suggestionDeclinedAt
        this.declinedAt = declinedAt
        this.validatedAt = validatedAt
    }
}
