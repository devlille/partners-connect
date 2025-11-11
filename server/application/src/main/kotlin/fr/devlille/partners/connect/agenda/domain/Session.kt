package fr.devlille.partners.connect.agenda.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enhanced Session domain entity for agenda response
 */
@Serializable
data class Session(
    val id: String,
    val name: String,
    val abstract: String?,
    @SerialName("start_time")
    val startTime: LocalDateTime?,
    @SerialName("end_time")
    val endTime: LocalDateTime?,
    @SerialName("track_name")
    val trackName: String?,
    val language: String?,
)
