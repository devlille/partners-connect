package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
data class AttachOptionsToPack(
    val required: List<String>,
    val optional: List<String>,
)
