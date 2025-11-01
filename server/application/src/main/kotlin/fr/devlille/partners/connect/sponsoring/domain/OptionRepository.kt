package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface OptionRepository {
    fun listOptionsByEvent(eventSlug: String, language: String): List<SponsoringOption>

    fun createOption(eventSlug: String, input: CreateSponsoringOption): UUID

    fun updateOption(eventSlug: String, optionId: UUID, input: CreateSponsoringOption): UUID

    fun deleteOption(eventSlug: String, optionId: UUID)

    fun attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack)

    fun detachOptionFromPack(eventSlug: String, packId: UUID, optionId: UUID)

    fun listOptionsByEventWithAllTranslations(eventSlug: String): List<SponsoringOptionWithTranslations>

    fun getOptionByIdWithAllTranslations(eventSlug: String, optionId: UUID): SponsoringOptionWithTranslations
}
