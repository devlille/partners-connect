package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface PackRepository {
    fun findPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>

    fun getById(eventSlug: String, packId: UUID, language: String): SponsoringPack

    fun createPack(eventSlug: String, input: CreateSponsoringPack): UUID

    fun updatePack(eventSlug: String, packId: UUID, input: CreateSponsoringPack): UUID

    fun deletePack(eventSlug: String, packId: UUID)
}
