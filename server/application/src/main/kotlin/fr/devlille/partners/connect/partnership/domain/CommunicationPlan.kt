package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunicationItem(
    val id: String,
    @SerialName("partnership_id")
    val partnershipId: String? = null,
    val title: String,
    @SerialName("publication_date")
    val publicationDate: LocalDateTime? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
)

@Serializable
data class CommunicationPlan(
    val done: List<CommunicationItem>,
    val planned: List<CommunicationItem>,
    val unplanned: List<CommunicationItem>,
)

@Serializable
data class CommunicationPlanEntry(
    val id: String,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("partnership_id")
    val partnershipId: String? = null,
    val title: String,
    @SerialName("scheduled_date")
    val scheduledDate: LocalDateTime? = null,
    val description: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
