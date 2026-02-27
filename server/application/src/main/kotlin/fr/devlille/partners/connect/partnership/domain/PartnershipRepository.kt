package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.partnership.infrastructure.api.PartnershipOrganiserResponse
import java.util.UUID

@Suppress("TooManyFunctions")
interface PartnershipRepository {
    fun register(eventSlug: String, register: RegisterPartnership): UUID

    fun getById(eventSlug: String, partnershipId: UUID): Partnership

    fun getByIdDetailed(eventSlug: String, partnershipId: UUID): PartnershipDetail

    fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company

    fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters = PartnershipFilters(),
        direction: String = "asc",
    ): PaginatedResponse<PartnershipItem>

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

    /**
     * Updates partnership contact information fields.
     *
     * Supports partial updates - only provided fields are updated, others remain unchanged.
     * All fields in UpdatePartnershipContactInfo are nullable to support this pattern.
     *
     * @param eventSlug Slug of the event that owns this partnership
     * @param partnershipId UUID of the partnership to update
     * @param update Contact information fields to update (all optional)
     * @return Updated partnership domain object with new contact information
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
     *   if event or partnership not found
     */
    fun updateContactInfo(
        eventSlug: String,
        partnershipId: UUID,
        update: UpdatePartnershipContactInfo,
    ): UUID

    /**
     * Deletes an unvalidated partnership.
     *
     * Only partnerships where both validatedAt and declinedAt are null can be deleted.
     * Performs a hard delete with no audit trail.
     *
     * @param partnershipId UUID of the partnership to delete
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
     *   if partnership not found
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
     *   if partnership has been finalized (validated or declined)
     */
    fun delete(partnershipId: UUID)

    /**
     * Updates price overrides for the pack and/or options of a partnership.
     *
     * Partial update semantics:
     * - [UpdatePartnershipPricing.packPriceOverride] is always applied: null clears any existing
     *   override, a non-null value sets/replaces it.
     * - [UpdatePartnershipPricing.optionsPriceOverrides] when non-null applies overrides only to
     *   the listed option IDs; all other option overrides are unchanged.
     *
     * @param eventSlug Slug of the event owning this partnership
     * @param partnershipId UUID of the partnership to update
     * @param pricing Pricing update payload
     * @return UUID of the updated partnership
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
     *   if the event, partnership, or any listed option ID is not found
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
     *   if a non-null [UpdatePartnershipPricing.packPriceOverride] is provided but the partnership
     *   has no validated sponsoring pack
     */
    fun updatePricing(
        eventSlug: String,
        partnershipId: UUID,
        pricing: UpdatePartnershipPricing,
    ): UUID
}
