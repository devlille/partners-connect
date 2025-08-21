package fr.devlille.partners.connect.events.domain

interface EventRepository {
    fun getAllEvents(): List<EventSummary>

    fun getBySlug(eventSlug: String): EventWithOrganisation

    fun findByOrgSlug(orgSlug: String): List<EventSummary>

    fun createEvent(orgSlug: String, event: Event): String

    fun updateEvent(eventSlug: String, orgSlug: String, event: Event): String

    fun findByUserEmail(userEmail: String): List<EventSummary>

    fun updateBoothPlanImageUrl(eventSlug: String, imageUrl: String)

    fun createExternalLink(eventSlug: String, request: CreateEventExternalLinkRequest): String

    fun deleteExternalLink(externalLinkId: String)
}
