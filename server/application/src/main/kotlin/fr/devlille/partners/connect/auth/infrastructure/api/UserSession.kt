package fr.devlille.partners.connect.auth.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val state: String, val token: String)
