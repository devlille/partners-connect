package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val name: String,
    @SerialName("start_time")
    val startTime: LocalDateTime,
    @SerialName("end_time")
    val endTime: LocalDateTime,
    @SerialName("submission_start_time")
    val submissionStartTime: LocalDateTime,
    @SerialName("submission_end_time")
    val submissionEndTime: LocalDateTime,
    val address: String,
    val contact: Contact,
    @SerialName("qanda_enabled")
    val qandaEnabled: Boolean = false,
    @SerialName("qanda_max_questions")
    val qandaMaxQuestions: Int? = null,
    @SerialName("qanda_max_answers")
    val qandaMaxAnswers: Int? = null,
)

@Serializable
data class Contact(
    val email: String,
    val phone: String? = null,
)
