package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun getById(eventId: UUID): Event

    fun createEvent(orgSlug: String, event: Event): UUID

    fun updateEvent(id: UUID, orgSlug: String, event: Event): UUID

    fun findByOrganizerId(userId: UUID): List<EventSummary>
}
