package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipSuggestionRepository {
    fun suggest(eventId: UUID, companyId: UUID, partnershipId: UUID, input: SuggestPartnership): UUID

    fun approve(eventId: UUID, companyId: UUID, partnershipId: UUID): UUID

    fun decline(eventId: UUID, companyId: UUID, partnershipId: UUID): UUID
}
