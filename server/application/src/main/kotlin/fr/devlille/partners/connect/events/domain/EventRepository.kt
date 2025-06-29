package fr.devlille.partners.connect.events.domain

interface EventRepository {
    fun getAllEvents(): List<EventSummaryEntity>
    fun createEvent(event: EventEntity)
    fun updateEvent(event: EventEntity)
}
