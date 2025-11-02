package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface PartnershipDecisionRepository {
    fun validate(eventSlug: String, partnershipId: UUID): UUID

    fun decline(eventSlug: String, partnershipId: UUID): UUID
}
