package fr.devlille.partners.connect.users.domain

import kotlinx.serialization.Serializable

@Serializable
class User(val displayName: String?, val pictureUrl: String?, val email: String)
