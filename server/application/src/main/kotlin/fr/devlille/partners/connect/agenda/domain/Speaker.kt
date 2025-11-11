package fr.devlille.partners.connect.agenda.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain representation of a speaker
 * Located in: server/application/src/main/kotlin/fr/devlille/partners/connect/agenda/domain/Speaker.kt
 */
@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val biography: String?,
    @SerialName("job_title")
    val jobTitle: String?,
    @SerialName("photo_url")
    val photoUrl: String?,
    val pronouns: String?,
)
