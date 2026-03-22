package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoothActivity(
    val id: String,
    @SerialName("partnership_id")
    val partnershipId: String,
    val title: String,
    val description: String,
    @SerialName("start_time")
    val startTime: LocalDateTime?,
    @SerialName("end_time")
    val endTime: LocalDateTime?,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
