package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QandaAnswer(
    val id: String,
    val answer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean,
)
