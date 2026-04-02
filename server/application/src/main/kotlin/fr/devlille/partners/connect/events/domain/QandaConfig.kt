package fr.devlille.partners.connect.events.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaConfig(
    @SerialName("max_questions")
    val maxQuestions: Int,
    @SerialName("max_answers")
    val maxAnswers: Int,
)
