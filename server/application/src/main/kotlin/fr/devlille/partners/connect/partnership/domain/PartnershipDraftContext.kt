package fr.devlille.partners.connect.partnership.domain

data class PartnershipDraftContext(
    val companyName: String,
    val packName: String?,
    val language: String,
    val status: String,
)
