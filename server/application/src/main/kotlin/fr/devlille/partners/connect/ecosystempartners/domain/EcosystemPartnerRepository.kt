package fr.devlille.partners.connect.ecosystempartners.domain

import java.util.UUID

interface EcosystemPartnerRepository {
    fun register(eventSlug: String, request: RegisterEcosystemPartner): UUID

    fun getById(eventSlug: String, ecosystemPartnerId: UUID): EcosystemPartner

    fun update(eventSlug: String, ecosystemPartnerId: UUID, request: UpdateEcosystemPartner): EcosystemPartner

    fun delete(eventSlug: String, ecosystemPartnerId: UUID)

    fun listByEvent(eventSlug: String, filters: EcosystemPartnerFilters): List<EcosystemPartnerItem>

    fun listPublic(eventSlug: String): List<PublicEcosystemPartnerGroup>
}
