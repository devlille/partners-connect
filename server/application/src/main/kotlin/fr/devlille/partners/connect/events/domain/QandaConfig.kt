package fr.devlille.partners.connect.events.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaConfig(
    @SerialName("max_questions")
    val maxQuestions: Int,
    @SerialName("max_answers")
    val maxAnswers: Int,
    @SerialName("submission_deadline")
    val submissionDeadline: LocalDateTime? = null,
)
