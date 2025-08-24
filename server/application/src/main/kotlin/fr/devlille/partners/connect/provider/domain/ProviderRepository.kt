package fr.devlille.partners.connect.provider.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

interface ProviderRepository {
    fun list(
        query: String? = null,
        sort: String? = "createdAt",
        direction: String? = "asc",
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<Provider>

    fun create(input: CreateProvider): UUID

    fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>

    fun detachFromEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>
}
