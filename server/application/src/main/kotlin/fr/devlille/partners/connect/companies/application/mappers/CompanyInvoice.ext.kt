package fr.devlille.partners.connect.companies.application.mappers

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.companies.domain.Contact
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity

fun BillingEntity.toDomain(): CompanyBillingData = CompanyBillingData(
    name = name,
    contact = Contact(
        firstName = contactFirstName,
        lastName = contactLastName,
        email = contactEmail,
    ),
    po = po,
)
