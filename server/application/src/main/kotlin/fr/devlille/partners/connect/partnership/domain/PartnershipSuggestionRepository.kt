package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipSuggestionRepository {
    fun suggest(eventId: UUID, partnershipId: UUID, input: SuggestPartnership): UUID

    fun approve(eventId: UUID, partnershipId: UUID): UUID

    fun decline(eventId: UUID, partnershipId: UUID): UUID
}
