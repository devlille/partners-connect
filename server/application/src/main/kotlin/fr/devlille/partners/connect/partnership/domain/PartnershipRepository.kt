package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import kotlinx.datetime.LocalDateTime
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

    fun updateCommunicationPublicationDate(
        eventSlug: String,
        partnershipId: UUID,
        publicationDate: LocalDateTime,
    ): UUID

    fun updateCommunicationSupportUrl(
        eventSlug: String,
        partnershipId: UUID,
        supportUrl: String,
    ): UUID
}
