package fr.devlille.partners.connect.sponsoring.domain

interface OptionRepository {
    fun listOptionsByEvent(eventId: String, language: String): List<SponsoringOption>

    fun createOption(eventId: String, input: CreateSponsoringOption): String

    fun deleteOption(eventId: String, optionId: String)

    fun attachOptionsToPack(eventId: String, packId: String, options: AttachOptionsToPack)

    fun detachOptionFromPack(eventId: String, packId: String, optionId: String)
}
