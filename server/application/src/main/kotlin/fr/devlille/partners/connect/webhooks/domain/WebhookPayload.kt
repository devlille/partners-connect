package fr.devlille.partners.connect.webhooks.domain

import fr.devlille.partners.connect.agenda.domain.Speaker
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.partnership.domain.BoothActivity
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import fr.devlille.partners.connect.partnership.domain.QandaQuestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WebhookPayload {
    abstract val eventType: WebhookEventType
    abstract val resourceType: WebhookResourceType
    abstract val resourceId: String
    abstract val company: Company
    abstract val event: EventSummary
    abstract val timestamp: String

    @Serializable
    @SerialName("partnership")
    data class Partnership(
        override val eventType: WebhookEventType,
        override val resourceId: String,
        override val company: Company,
        override val event: EventSummary,
        override val timestamp: String,
        val partnership: PartnershipDetail,
        val jobs: List<JobOffer>,
        val activities: List<BoothActivity>,
        val questions: List<QandaQuestion>,
        val speakers: List<Speaker>,
        val supportVideoUrl: String? = null,
    ) : WebhookPayload() {
        override val resourceType: WebhookResourceType = WebhookResourceType.PARTNERSHIP
    }

    @Serializable
    @SerialName("ecosystem_partner")
    data class EcosystemPartner(
        override val eventType: WebhookEventType,
        override val resourceId: String,
        override val company: Company,
        override val event: EventSummary,
        override val timestamp: String,
        val category: String,
    ) : WebhookPayload() {
        override val resourceType: WebhookResourceType = WebhookResourceType.ECOSYSTEM_PARTNER
    }
}
