package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipBoothRepository {
    fun updateBoothLocation(eventSlug: String, partnershipId: UUID, location: String)
}
