package fr.devlille.partners.connect.sponsoring.domain

interface PackRepository {
    fun findPacksByEvent(eventId: String, language: String): List<SponsoringPack>

    fun createPack(eventId: String, input: CreateSponsoringPack): String

    fun deletePack(eventId: String, packId: String)
}
