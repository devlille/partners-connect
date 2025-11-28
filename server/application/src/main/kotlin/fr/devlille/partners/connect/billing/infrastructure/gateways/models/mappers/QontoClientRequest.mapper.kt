package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoBillingAddress
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientRequest
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity

internal fun BillingEntity.toQontoClientRequest(company: CompanyEntity): QontoClientRequest {
    val errors = mutableListOf<String>()
    if (company.vat.isNullOrBlank()) errors.add("vat")
    if (company.siret.isNullOrBlank()) errors.add("siret")
    if (company.address.isNullOrBlank()) errors.add("address")
    if (company.city.isNullOrBlank()) errors.add("city")
    if (company.zipCode.isNullOrBlank()) errors.add("zipCode")
    if (company.country.isNullOrBlank()) errors.add("country")
    if (errors.isNotEmpty()) {
        throw ForbiddenException(
            "Missing required company fields for Qonto client creation: ${errors.joinToString(", ")}",
        )
    }
    return QontoClientRequest(
        name = this.name ?: company.name,
        firstName = this.contactFirstName,
        lastName = this.contactLastName,
        type = "company",
        email = this.contactEmail,
        extraEmails = listOf(),
        vatNumber = company.vat,
        taxId = company.siret,
        billingAddress = QontoBillingAddress(
            streetAddress = company.address,
            city = company.city,
            zipCode = company.zipCode,
            countryCode = company.country,
        ),
        currency = "EUR",
        locale = "FR",
    )
}
