package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

data class GeneratedFlyer(val url: String)

interface FlyerGenerationRepository {
    /**
     * Generates a flyer for the given partnership and stores the URL on the partnership's
     * communication_support_url column. Throws:
     *  - NotFoundException if the partnership or pack is not found.
     *  - ConflictException if the partnership is not validated, the company has no logo,
     *    or the pack is not flyer-enabled.
     *  - IOException if the template or logo download fails.
     */
    suspend fun generate(eventSlug: String, partnershipId: UUID): GeneratedFlyer
}
