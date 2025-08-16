package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface OptionRepository {
    fun listOptionsByEvent(eventId: UUID, language: String): List<SponsoringOption>

    fun createOption(eventId: UUID, input: CreateSponsoringOption): UUID

    fun updateOption(eventId: UUID, optionId: UUID, input: CreateSponsoringOption): UUID

    fun deleteOption(eventId: UUID, optionId: UUID)

    fun attachOptionsToPack(eventId: UUID, packId: UUID, options: AttachOptionsToPack)

    fun detachOptionFromPack(eventId: UUID, packId: UUID, optionId: UUID)
}
