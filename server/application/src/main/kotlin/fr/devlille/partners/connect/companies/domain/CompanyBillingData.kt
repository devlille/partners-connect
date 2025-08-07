package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CompanyBillingData(
    val name: String? = null,
    val po: String? = null,
    val contact: Contact,
)

@Serializable
class Contact(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val email: String,
)
