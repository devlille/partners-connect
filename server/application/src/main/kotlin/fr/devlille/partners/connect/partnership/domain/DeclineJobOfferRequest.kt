package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.Serializable

/**
 * Request to decline a pending job offer promotion.
 *
 * This endpoint requires organizer authentication and canEdit permission. An optional
 * decline reason can be provided to give feedback to the partner about why their
 * job offer was not accepted for promotion.
 *
 * After decline, the promotion status changes to DECLINED, review metadata is set,
 * and notifications are sent including the reason if provided.
 *
 * @property reason Optional explanation for declining the promotion (max 500 chars recommended)
 */
@Serializable
data class DeclineJobOfferRequest(
    val reason: String? = null,
)
