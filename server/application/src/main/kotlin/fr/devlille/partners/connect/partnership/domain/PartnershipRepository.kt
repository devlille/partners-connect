package fr.devlille.partners.connect.partnership.domain

interface PartnershipRepository {
    fun register(eventId: String, companyId: String, register: RegisterPartnership): String
}
