package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CompanyInvoice(
    val name: String? = null,
    val po: String? = null,
    val vat: String,
    val siret: String,
    val address: Address,
    val contact: Contact,
)

@Serializable
class Address(
    val address: String,
    val city: String,
    @SerialName("zip_code")
    val zipCode: String,
    val country: String,
)

@Serializable
class Contact(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
)
