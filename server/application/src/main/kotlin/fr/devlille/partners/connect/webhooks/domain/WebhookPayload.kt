package fr.devlille.partners.connect.webhooks.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import kotlinx.serialization.Serializable

@Serializable
data class WebhookPayload(
    val eventType: WebhookEventType,
    val partnership: PartnershipDetail,
    val company: Company,
    val event: EventSummary,
    val jobs: List<JobOffer>,
    val timestamp: String,
)
