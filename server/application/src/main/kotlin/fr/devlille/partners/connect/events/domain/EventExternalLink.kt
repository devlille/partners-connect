package fr.devlille.partners.connect.events.domain

import kotlinx.serialization.Serializable

@Serializable
data class EventExternalLink(
    val id: String,
    val name: String,
    val url: String,
)

@Serializable
data class CreateEventExternalLinkRequest(
    val name: String,
    val url: String,
)
