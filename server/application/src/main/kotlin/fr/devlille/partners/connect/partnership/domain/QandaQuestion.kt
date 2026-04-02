package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaQuestion(
    val id: String,
    @SerialName("partnership_id")
    val partnershipId: String,
    val question: String,
    val answers: List<QandaAnswer>,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
