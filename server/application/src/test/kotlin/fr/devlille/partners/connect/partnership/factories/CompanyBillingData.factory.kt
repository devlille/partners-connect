package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.companies.domain.CompanyBillingData
import fr.devlille.partners.connect.companies.domain.Contact

fun createCompanyBillingData(
    name: String? = "DevLille SAS",
    po: String? = "PO1234",
    firstName: String = "Jean",
    lastName: String = "Dupont",
    email: String = "jean.dupont@example.com",
): CompanyBillingData = CompanyBillingData(
    name = name,
    po = po,
    contact = Contact(
        firstName = firstName,
        lastName = lastName,
        email = email,
    ),
)
