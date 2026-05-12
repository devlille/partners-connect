package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface SupportVideoRepository {
    /**
     * Records the partner's support video URL (already uploaded to GCS by the route).
     *
     * - 404 if event or partnership not found.
     * - 409 if the partnership's current video is already APPROVED.
     * - If a prior video exists in PENDING/DECLINED state, the URL is replaced and the status
     *   is reset to PENDING (review metadata cleared).
     */
    fun submit(eventSlug: String, partnershipId: UUID, url: String): UUID

    /** Returns the partnership's current support video (any status), or throws NotFoundException. */
    fun get(eventSlug: String, partnershipId: UUID): SupportVideoResponse
}
