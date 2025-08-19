package fr.devlille.partners.connect.organisations.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organisation(
    val name: String,
    @SerialName("head_office")
    val headOffice: String? = null,
    val siret: String? = null,
    val siren: String? = null,
    val tva: String? = null,
    @SerialName("d_and_b")
    val dAndB: String? = null,
    val nace: String? = null,
    val naf: String? = null,
    val duns: String? = null,
    val iban: String? = null,
    val bic: String? = null,
    @SerialName("rib_url")
    val ribUrl: String? = null,
    @SerialName("representative_user_email")
    val representativeUserEmail: String? = null,
    @SerialName("representative_role")
    val representativeRole: String? = null,
    @SerialName("creation_location")
    val creationLocation: String? = null,
    @SerialName("created_at")
    val createdAt: LocalDateTime? = null,
    @SerialName("published_at")
    val publishedAt: LocalDateTime? = null,
)
