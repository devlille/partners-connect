package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.Serializable

/**
 * Request body for sending bulk emails to filtered partnerships.
 *
 * @property subject Email subject line (will be prefixed with [event_name])
 * @property body Email body content in HTML format
 */
@Serializable
data class SendPartnershipEmailRequest(
    val subject: String,
    val body: String,
)
