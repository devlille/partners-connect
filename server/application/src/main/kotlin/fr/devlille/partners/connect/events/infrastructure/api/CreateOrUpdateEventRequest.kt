package fr.devlille.partners.connect.events.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateEventRequest(
    val name: String,
    val start_time: String,
    val end_time: String,
    val submission_start_time: String,
    val submission_end_time: String,
    val address: String,
    val contact: Contact,
    val legal: Legal? = null,
    val banking: Banking? = null
)

@Serializable
data class Contact(
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class Legal(
    val name: String? = null,
    val siret: String? = null,
    val siren: String? = null,
    val tva: String? = null,
    val d_and_b: String? = null,
    val nace: String? = null,
    val naf: String? = null,
    val duns: String? = null
)

@Serializable
data class Banking(
    val iban: String? = null,
    val bic: String? = null,
    val rib_url: String? = null
)
