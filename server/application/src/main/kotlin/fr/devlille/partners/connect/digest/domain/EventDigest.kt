package fr.devlille.partners.connect.digest.domain

import fr.devlille.partners.connect.events.domain.EventWithOrganisation

/**
 * The full digest payload for a single event.
 *
 * Each list represents one category of actionable items.
 * If all lists are empty the digest must not be sent.
 *
 * @param event The event this digest belongs to (used to look up the Slack channel).
 * @param agreementItems Partnerships ready for agreement generation.
 * @param billingItems Partnerships ready for billing generation (no quote or invoice yet).
 * @param socialMediaItems Partnerships scheduled for social media communication today.
 */
data class EventDigest(
    val event: EventWithOrganisation,
    val agreementItems: List<DigestEntry>,
    val billingItems: List<DigestEntry>,
    val socialMediaItems: List<DigestEntry>,
    val jobOfferItems: List<DigestEntry>,
) {
    /** `true` when at least one section has items; used by the route to gate Slack dispatch. */
    val hasItems: Boolean
        get() = agreementItems.isNotEmpty() || billingItems.isNotEmpty() ||
            socialMediaItems.isNotEmpty() || jobOfferItems.isNotEmpty()
}
