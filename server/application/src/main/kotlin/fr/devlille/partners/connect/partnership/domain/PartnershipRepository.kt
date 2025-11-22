package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.partnership.infrastructure.api.PartnershipOrganiserResponse
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

    /**
     * Assigns an organiser to a partnership.
     *
     * @param partnershipId UUID of the partnership
     * @param email Email of the user to assign as organiser
     * @return Updated partnership organiser response with assigned organiser
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
     *   if partnership or user not found
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
     *   if user is not a member of the organisation or lacks edit permission
     */
    fun assignOrganiser(partnershipId: UUID, email: String): PartnershipOrganiserResponse

    /**
     * Removes the organiser assignment from a partnership.
     *
     * @param partnershipId UUID of the partnership
     * @return Updated partnership organiser response without organiser
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException if partnership not found
     */
    fun removeOrganiser(partnershipId: UUID): PartnershipOrganiserResponse
}
