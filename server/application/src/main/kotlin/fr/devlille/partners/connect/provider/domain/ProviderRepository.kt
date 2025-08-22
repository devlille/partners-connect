package fr.devlille.partners.connect.provider.domain

import java.util.UUID

interface ProviderRepository {
    fun list(query: String? = null, sort: String? = "createdAt", direction: String? = "asc"): List<Provider>

    fun create(input: CreateProvider): UUID

    fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>

    fun detachFromEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>
}
