package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface PackRepository {
    fun findPacksByEvent(eventId: UUID, language: String): List<SponsoringPack>

    fun getById(eventId: UUID, packId: UUID, language: String): SponsoringPack

    fun createPack(eventId: UUID, input: CreateSponsoringPack): UUID

    fun updatePack(eventId: UUID, packId: UUID, input: CreateSponsoringPack): UUID

    fun deletePack(eventId: UUID, packId: UUID)
}
