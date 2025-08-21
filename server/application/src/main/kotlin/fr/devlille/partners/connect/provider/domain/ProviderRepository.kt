package fr.devlille.partners.connect.provider.domain

import java.util.UUID

interface ProviderRepository {
    fun list(): List<Provider>

    fun create(input: CreateProvider): Provider

    fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID>

    fun validateProviderIds(providerIds: List<UUID>): Boolean
}
