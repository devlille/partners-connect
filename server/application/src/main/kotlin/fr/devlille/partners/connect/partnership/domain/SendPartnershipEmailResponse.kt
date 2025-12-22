package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.Serializable

/**
 * Response after successfully sending emails to partnerships.
 *
 * @property recipients Total number of unique email recipients who received the email
 */
@Serializable
data class SendPartnershipEmailResponse(
    val recipients: Int,
)
