package fr.devlille.partners.connect.provider.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

/**
 * Repository interface for provider data access operations.
 *
 * Provides organisation-scoped access to provider entities with support for CRUD operations,
 * event attachments, and public listing. All operations except public listing enforce
 * organisation-level access control.
 */
interface ProviderRepository {
    /**
     * List providers with optional organization scoping and filtering.
     *
     * @param orgSlug Optional organisation filter - if provided, only returns providers
     *                for that organisation. If null, returns all public providers.
     * @param query Optional text search filter for provider names/descriptions
     * @param sort Sort field (default: "createdAt")
     * @param direction Sort direction: "asc" or "desc" (default: "asc")
     * @param page Page number for pagination (1-based, default: 1)
     * @param pageSize Number of items per page (default: 20)
     * @return Paginated response containing matching providers
     */
    @Suppress("LongParameterList")
    fun list(
        orgSlug: String? = null,
        query: String? = null,
        sort: String? = "createdAt",
        direction: String? = "asc",
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<Provider>

    /**
     * Get a specific provider by ID within organisation scope.
     *
     * @param providerId Unique identifier for the provider
     * @param orgSlug Organisation slug for access control
     * @return Provider if found and accessible
     * @throws NoSuchElementException if provider not found or not in organisation
     */
    fun findByIdAndOrganisation(providerId: UUID, orgSlug: String): Provider

    /**
     * Find all providers attached to a specific event.
     *
     * @param eventSlug Event identifier for provider lookup
     * @param page Page number for pagination (1-based, default: 1)
     * @param pageSize Number of items per page (default: 20)
     * @return Paginated response containing event's attached providers
     */
    fun findByEvent(eventSlug: String, page: Int = 1, pageSize: Int = 20): PaginatedResponse<Provider>

    /**
     * Create a new provider within organisation scope.
     *
     * @param input Provider creation data
     * @param orgSlug Organisation slug for ownership context
     * @return UUID of the created provider
     */
    fun create(input: CreateProvider, orgSlug: String): UUID

    /**
     * Update an existing provider within organisation scope.
     *
     * @param providerId Unique identifier of provider to update
     * @param input Provider update data (partial updates supported)
     * @param orgSlug Organisation slug for access control
     * @return Updated provider data
     * @throws NoSuchElementException if provider not found or not in organisation
     */
    fun update(providerId: UUID, input: UpdateProvider, orgSlug: String): Provider

    /**
     * Delete a provider within organisation scope.
     *
     * Must check for event attachments before deletion to prevent orphaned relationships.
     *
     * @param providerId Unique identifier of provider to delete
     * @param orgSlug Organisation slug for access control
     * @return true if deleted successfully, false if not found
     * @throws IllegalStateException if provider has active event attachments
     */
    fun delete(providerId: UUID, orgSlug: String): Boolean

    /**
     * Check if provider has any event attachments.
     *
     * Used for deletion validation to prevent orphaned event-provider relationships.
     *
     * @param providerId Unique identifier of the provider
     * @return true if provider is attached to any events
     */
    fun hasEventAttachments(providerId: UUID): Boolean

    /**
     * Attach providers to an event within organisation scope.
     *
     * @param orgSlug Organisation slug for access control
     * @param eventSlug Event identifier for attachment
     * @param providerIds List of provider UUIDs to attach
     * @return List of successfully attached provider UUIDs
     */
    fun attachToEvent(orgSlug: String, eventSlug: String, providerIds: List<UUID>): List<UUID>

    /**
     * Detach providers from an event within organisation scope.
     *
     * @param orgSlug Organisation slug for access control
     * @param eventSlug Event identifier for detachment
     * @param providerIds List of provider UUIDs to detach
     * @return List of successfully detached provider UUIDs
     */
    fun detachFromEvent(orgSlug: String, eventSlug: String, providerIds: List<UUID>): List<UUID>
}
