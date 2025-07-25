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
    val legal: Legal? = null,
    val banking: Banking? = null,
)

@Serializable
data class Contact(
    val email: String,
    val phone: String? = null,
)

@Serializable
data class Legal(
    val name: String? = null,
    val siret: String? = null,
    val siren: String? = null,
    val tva: String? = null,
    @SerialName("d_and_b")
    val dAndB: String? = null,
    val nace: String? = null,
    val naf: String? = null,
    val duns: String? = null,
)

@Serializable
data class Banking(
    val iban: String? = null,
    val bic: String? = null,
    @SerialName("rib_url")
    val ribUrl: String? = null,
)
