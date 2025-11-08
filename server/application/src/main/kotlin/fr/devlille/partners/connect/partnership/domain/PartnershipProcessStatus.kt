package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Detailed workflow status tracking with timestamps for all process phases.
 * Used in partnership detail responses to show the current state and history
 * of the partnership workflow.
 */
@Serializable
data class PartnershipProcessStatus(
    @SerialName("suggestion_sent_at")
    val suggestionSentAt: String? = null,
    @SerialName("suggestion_approved_at")
    val suggestionApprovedAt: String? = null,
    @SerialName("suggestion_declined_at")
    val suggestionDeclinedAt: String? = null,
    @SerialName("validated_at")
    val validatedAt: String? = null,
    @SerialName("declined_at")
    val declinedAt: String? = null,
    @SerialName("agreement_url")
    val agreementUrl: String? = null,
    @SerialName("agreement_signed_url")
    val agreementSignedUrl: String? = null,
    @SerialName("communication_publication_date")
    val communicationPublicationDate: String? = null,
    @SerialName("communication_support_url")
    val communicationSupportUrl: String? = null,
    @SerialName("billing_status")
    val billingStatus: String? = null,
)
