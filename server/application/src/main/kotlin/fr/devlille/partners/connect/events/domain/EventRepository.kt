package fr.devlille.partners.connect.events.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

interface EventRepository {
    fun getAllEvents(page: Int, pageSize: Int): PaginatedResponse<EventSummary>

    fun getBySlug(eventSlug: String): EventWithOrganisation

    fun findByOrgSlug(orgSlug: String): List<EventSummary>

    fun findByOrgSlugPaginated(orgSlug: String, page: Int, pageSize: Int): PaginatedResponse<EventSummary>

    fun createEvent(orgSlug: String, event: Event): String

    fun updateEvent(eventSlug: String, orgSlug: String, event: Event): String

    fun findByUserEmail(userEmail: String): List<EventSummary>

    fun updateBoothPlanImageUrl(eventSlug: String, imageUrl: String)

    fun createExternalLink(eventSlug: String, request: CreateEventExternalLinkRequest): UUID

    fun deleteExternalLink(externalLinkId: UUID)
}
