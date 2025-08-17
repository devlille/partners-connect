package fr.devlille.partners.connect.events.domain

import java.util.UUID

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    // Slug-based methods (new)
    fun getBySlug(eventSlug: String): Event

    fun getPublicEventBySlug(eventSlug: String): EventWithOrganisation

    // UUID-based methods (for backward compatibility with other modules)
    fun getById(eventId: UUID): Event

    fun getPublicEventById(eventId: UUID): EventWithOrganisation

    // Helper method to convert slug to UUID for internal use
    fun getIdBySlug(eventSlug: String): UUID

    fun findByOrgSlug(orgSlug: String): List<EventSummary>

    fun createEvent(orgSlug: String, event: Event): String

    fun updateEvent(eventSlug: String, orgSlug: String, event: Event): String

    fun findByUserEmail(userEmail: String): List<EventSummary>
}
