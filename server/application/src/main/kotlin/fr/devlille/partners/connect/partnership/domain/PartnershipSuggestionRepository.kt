package fr.devlille.partners.connect.partnership.domain

interface PartnershipSuggestionRepository {
    fun suggest(eventId: String, companyId: String, partnershipId: String, input: SuggestPartnership): String
}
