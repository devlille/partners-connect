package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface PackRepository {
    fun findPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>

    fun createPack(eventSlug: String, input: CreateSponsoringPack): UUID

    fun updatePack(eventSlug: String, packId: UUID, input: CreateSponsoringPack): UUID

    fun deletePack(eventSlug: String, packId: UUID)

    fun findPacksByEventWithAllTranslations(eventSlug: String): List<SponsoringPackWithTranslations>
}
