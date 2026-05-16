package fr.devlille.partners.connect.ecosystempartners.domain

import java.util.UUID

interface EcosystemPartnerDecisionRepository {
    fun validate(eventSlug: String, ecosystemPartnerId: UUID): UUID

    fun decline(eventSlug: String, ecosystemPartnerId: UUID): UUID
}
