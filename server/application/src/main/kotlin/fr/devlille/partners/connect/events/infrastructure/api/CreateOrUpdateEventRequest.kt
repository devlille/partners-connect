package fr.devlille.partners.connect.events.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateEventRequest(
    val name: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("submission_start_time")
    val submissionStartTime: String,
    @SerialName("submission_end_time")
    val submissionEndTime: String,
    val address: String,
    val contact: Contact,
    val legal: Legal? = null,
    val banking: Banking? = null,
)

@Serializable
data class Contact(
    val phone: String? = null,
    val email: String? = null,
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
