package fr.devlille.partners.connect.partnership.domain

data class PartnershipAgreement(
    val path: String,
    val organisation: Organisation,
    val event: Event,
    val company: Company,
    val partnership: PartnershipInfo,
    val location: String,
    val createdAt: String,
)

data class Event(
    val name: String,
    val paymentDeadline: String,
    val endDate: String,
)

data class Organisation(
    val name: String,
    val headOffice: String,
    val iban: String,
    val bic: String,
    val creationLocation: String,
    val createdAt: String,
    val publishedAt: String,
    val representative: ContactInfo,
)

data class ContactInfo(
    val name: String,
    val role: String,
)

data class Company(
    val name: String,
    val siret: String,
    val headOffice: String,
)

data class PartnershipInfo(
    val hasBooth: Boolean,
    val contact: ContactInfo,
)
