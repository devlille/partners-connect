package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun createEvent(event: Event): UUID

    fun updateEvent(event: Event): UUID
}
