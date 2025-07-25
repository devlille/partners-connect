package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun getById(eventId: UUID): Event

    fun createEvent(event: Event): UUID

    fun updateEvent(id: UUID, event: Event): UUID
}
