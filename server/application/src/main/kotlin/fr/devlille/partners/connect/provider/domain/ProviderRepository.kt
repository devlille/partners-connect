package fr.devlille.partners.connect.provider.domain

import java.util.UUID

interface ProviderRepository {
    fun list(sort: String? = null, direction: String? = null, query: String? = null): List<Provider>

    fun create(input: CreateProvider): UUID

    fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>

    fun detachFromEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>
}
