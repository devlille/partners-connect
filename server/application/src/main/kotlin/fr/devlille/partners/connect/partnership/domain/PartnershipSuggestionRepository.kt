package fr.devlille.partners.connect.partnership.domain

interface PartnershipSuggestionRepository {
    fun suggest(eventId: String, companyId: String, partnershipId: String, input: SuggestPartnership): String

    fun approve(eventId: String, companyId: String, partnershipId: String): String

    fun decline(eventId: String, companyId: String, partnershipId: String): String
}
