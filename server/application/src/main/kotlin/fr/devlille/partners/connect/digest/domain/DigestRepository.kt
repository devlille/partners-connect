package fr.devlille.partners.connect.digest.domain

import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.LocalDate

/**
 * Repository that computes the daily digest payload for a single event.
 */
interface DigestRepository {
    /**
     * Returns the digest payload for a single event, including all three readiness
     * categories: agreement-ready, quote-ready, and social-media-due today.
     *
     * @param eventSlug The slug of the event for which to compute the digest.
     * @param today Today's UTC date, used for social media date comparison.
     * @return [EventDigest] containing the aggregated items across all categories.
     * @throws NotFoundException if the event does not exist.
     */
    suspend fun queryDigest(eventSlug: String, today: LocalDate): EventDigest
}
