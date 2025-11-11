package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain representation of a speaker-partnership association
 */
@Serializable
data class SpeakerPartnership(
    val id: String,
    @SerialName("speaker_id")
    val speakerId: String,
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
