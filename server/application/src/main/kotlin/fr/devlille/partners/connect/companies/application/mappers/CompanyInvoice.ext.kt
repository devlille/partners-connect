package fr.devlille.partners.connect.companies.application.mappers

import fr.devlille.partners.connect.companies.domain.Address
import fr.devlille.partners.connect.companies.domain.CompanyInvoice
import fr.devlille.partners.connect.companies.domain.Contact
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceEntity

fun InvoiceEntity.toDomain(): CompanyInvoice = CompanyInvoice(
    name = name,
    contact = Contact(
        firstName = contactFirstName,
        lastName = contactLastName,
        email = contactEmail,
    ),
    address = Address(
        address = address,
        city = city,
        zipCode = zipCode,
        country = country,
    ),
    siret = siret,
    vat = vat,
    po = po,
)
