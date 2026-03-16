package fr.devlille.partners.connect.partnership.domain

interface PartnershipCommunicationRepository {
    fun listCommunicationPlan(eventSlug: String): CommunicationPlan
}
