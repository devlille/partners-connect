package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun getById(eventId: String): Event

    fun createEvent(event: Event): UUID

    fun updateEvent(event: Event): UUID
}
