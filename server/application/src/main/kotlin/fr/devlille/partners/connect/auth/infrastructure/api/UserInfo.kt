package fr.devlille.partners.connect.auth.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserInfo(
    @SerialName("display_name")
    val displayName: String,
    @SerialName("picture_url")
    val pictureUrl: String,
    val email: String,
)
