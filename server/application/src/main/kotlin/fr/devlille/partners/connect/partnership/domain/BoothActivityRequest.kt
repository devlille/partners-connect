package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoothActivityRequest(
    val title: String,
    val description: String,
    @SerialName("start_time")
    val startTime: LocalDateTime? = null,
    @SerialName("end_time")
    val endTime: LocalDateTime? = null,
)
