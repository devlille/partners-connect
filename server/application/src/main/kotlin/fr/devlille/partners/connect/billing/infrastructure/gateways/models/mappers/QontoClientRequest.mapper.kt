package fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoBillingAddress
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientRequest
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity

internal fun BillingEntity.toQontoClientRequest(company: CompanyEntity): QontoClientRequest {
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
