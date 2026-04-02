package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaQuestionRequest(
    val question: String,
    val answers: List<QandaAnswerInput>,
)

@Serializable
data class QandaAnswerInput(
    val answer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean,
)
