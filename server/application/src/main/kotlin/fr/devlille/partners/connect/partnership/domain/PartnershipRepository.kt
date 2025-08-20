package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import java.util.UUID

interface PartnershipRepository {
    fun register(eventSlug: String, register: RegisterPartnership): UUID

    fun getById(eventSlug: String, partnershipId: UUID): Partnership

    fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company

    fun validate(eventSlug: String, partnershipId: UUID): UUID

    fun decline(eventSlug: String, partnershipId: UUID): UUID

    fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters = PartnershipFilters(),
        sort: String = "created",
        direction: String = "asc",
    ): List<PartnershipItem>

    fun listByCompany(companyId: UUID): List<PartnershipItem>

    // Booth management methods
    fun updateBoothLocation(eventSlug: String, partnershipId: UUID, location: String)
}
