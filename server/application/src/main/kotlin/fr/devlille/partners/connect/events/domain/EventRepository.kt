package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun getById(eventId: UUID): Event

    fun findByOrgSlug(orgSlug: String): List<EventSummary>

    fun createEvent(orgSlug: String, event: Event): UUID

    fun updateEvent(id: UUID, orgSlug: String, event: Event): UUID
}
