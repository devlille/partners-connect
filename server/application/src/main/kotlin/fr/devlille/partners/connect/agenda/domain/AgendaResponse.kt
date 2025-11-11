package fr.devlille.partners.connect.agenda.domain

import kotlinx.serialization.Serializable

/**
 * Response for retrieving imported agenda data
 * GET /orgs/{orgSlug}/events/{eventSlug}/agenda
 */
@Serializable
data class AgendaResponse(
    val sessions: List<Session>,
    val speakers: List<Speaker>,
)
