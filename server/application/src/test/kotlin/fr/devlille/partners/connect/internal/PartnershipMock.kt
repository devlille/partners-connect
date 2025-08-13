package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockPartnership(
    id: UUID = UUID.randomUUID(),
    event: EventEntity = insertMockedEvent(),
    company: CompanyEntity = insertMockedCompany(),
    phone: String? = null,
    contactName: String = "John Doe",
    contactRole: String = "Developer",
    language: String = "en",
    agreementUrl: String? = null,
    agreementSignedUrl: String? = null,
    selectedPack: SponsoringPackEntity? = null,
    suggestionPack: SponsoringPackEntity? = null,
    suggestionSentAt: LocalDateTime? = null,
    suggestionApprovedAt: LocalDateTime? = null,
    suggestionDeclinedAt: LocalDateTime? = null,
    declinedAt: LocalDateTime? = null,
    validatedAt: LocalDateTime? = null,
): PartnershipEntity = transaction {
    PartnershipEntity.new(id) {
        this.event = event
        this.company = company
        this.phone = phone
        this.contactName = contactName
        this.contactRole = contactRole
        this.language = language
        this.agreementUrl = agreementUrl
        this.agreementSignedUrl = agreementSignedUrl
        this.selectedPack = selectedPack
        this.suggestionPack = suggestionPack
        this.suggestionSentAt = suggestionSentAt
        this.suggestionApprovedAt = suggestionApprovedAt
        this.suggestionDeclinedAt = suggestionDeclinedAt
        this.declinedAt = declinedAt
        this.validatedAt = validatedAt
    }
}
