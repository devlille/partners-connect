package fr.devlille.partners.connect.users.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class User(
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("picture_url")
    val pictureUrl: String?,
    val email: String,
)
