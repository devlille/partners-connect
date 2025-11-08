package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import java.util.UUID

interface PartnershipRepository {
    fun register(eventSlug: String, register: RegisterPartnership): UUID

    fun getById(eventSlug: String, partnershipId: UUID): Partnership

    fun getByIdDetailed(eventSlug: String, partnershipId: UUID): PartnershipDetail

    fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company

    fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters = PartnershipFilters(),
        direction: String = "asc",
    ): List<PartnershipItem>

    fun listByCompany(companyId: UUID): List<PartnershipItem>
}
