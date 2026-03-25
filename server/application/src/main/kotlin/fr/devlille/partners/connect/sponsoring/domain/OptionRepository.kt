package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface OptionRepository {
    fun createOption(eventSlug: String, input: CreateSponsoringOption): UUID

    fun updateOption(eventSlug: String, optionId: UUID, input: CreateSponsoringOption): UUID

    fun deleteOption(eventSlug: String, optionId: UUID)

    fun attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack)

    fun detachOptionFromPack(eventSlug: String, packId: UUID, optionId: UUID)

    fun listOptionsByEventWithAllTranslations(eventSlug: String): List<SponsoringOptionWithTranslations>

    fun listOptionsWithPartnershipCounts(eventSlug: String): List<SponsoringOptionWithCount>

    fun getOptionByIdWithAllTranslations(eventSlug: String, optionId: UUID): SponsoringOptionWithTranslations

    fun getOptionByIdWithPartners(eventSlug: String, optionId: UUID): SponsoringOptionDetailWithPartners
}
