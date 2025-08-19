package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipSuggestionRepository {
    fun suggest(eventSlug: String, partnershipId: UUID, input: SuggestPartnership): UUID

    fun approve(eventSlug: String, partnershipId: UUID): UUID

    fun decline(eventSlug: String, partnershipId: UUID): UUID
}
