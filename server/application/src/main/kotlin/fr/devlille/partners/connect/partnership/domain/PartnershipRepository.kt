package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.companies.domain.Company
import kotlinx.datetime.LocalDateTime
import java.util.UUID

@Suppress("TooManyFunctions") // Required for comprehensive partnership management operations
interface PartnershipRepository {
    fun register(eventSlug: String, register: RegisterPartnership): UUID

    fun getById(eventSlug: String, partnershipId: UUID): Partnership

    fun getCompanyByPartnershipId(eventSlug: String, partnershipId: UUID): Company

    /**
     * Validates a partnership with optional customization of package details.
     *
     * Allows organizers to override the selected pack's default values for:
     * - Number of tickets (nbTickets)
     * - Number of job offers (nbJobOffers)
     * - Booth size (boothSize)
     *
     * If [request] is null, all values are taken from the selected pack's defaults.
     * If [request] is provided, specified fields override pack defaults, while null fields use pack defaults.
     *
     * Validation rules:
     * - Cannot re-validate after agreement is signed
     *   (throws [fr.devlille.partners.connect.internal.infrastructure.api.ConflictException])
     * - Custom booth size must exist in at least one pack for the event
     *   (throws [io.ktor.server.plugins.BadRequestException])
     * - All numeric values must be >= 0 (enforced by JSON schema validation)
     *
     * @param eventSlug The event slug
     * @param partnershipId The partnership UUID
     * @param request Optional validation request with custom values.
     *                If null, uses all pack defaults.
     * @return The partnership UUID
     * @throws io.ktor.server.plugins.NotFoundException if event or partnership not found
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
     *         if agreement already signed
     * @throws io.ktor.server.plugins.BadRequestException
     *         if custom booth size is not available in any pack
     */
    fun validate(eventSlug: String, partnershipId: UUID, request: ValidatePartnershipRequest? = null): UUID

    fun decline(eventSlug: String, partnershipId: UUID): UUID

    fun listByEvent(
        eventSlug: String,
        filters: PartnershipFilters = PartnershipFilters(),
        sort: String = "created",
        direction: String = "asc",
    ): List<PartnershipItem>

    fun listByCompany(companyId: UUID): List<PartnershipItem>

    fun updateBoothLocation(eventSlug: String, partnershipId: UUID, location: String)

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

    fun listCommunicationPlan(eventSlug: String): CommunicationPlan
}
