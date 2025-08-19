package fr.devlille.partners.connect.organisations.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organisation(
    val name: String,
    val slug: String,
    @SerialName("head_office")
    val headOffice: String,
    val siret: String? = null,
    val siren: String? = null,
    val tva: String? = null,
    @SerialName("d_and_b")
    val dAndB: String? = null,
    val nace: String? = null,
    val naf: String? = null,
    val duns: String? = null,
    val iban: String,
    val bic: String,
    @SerialName("rib_url")
    val ribUrl: String,
    @SerialName("representative_user_email")
    val representativeUserEmail: String,
    @SerialName("representative_role")
    val representativeRole: String,
    @SerialName("creation_location")
    val creationLocation: String,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("published_at")
    val publishedAt: LocalDateTime,
)
