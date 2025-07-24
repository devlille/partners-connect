package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack

sealed interface NotificationVariables {
    val usageName: String
    val language: String

    fun populate(content: String): String

    class NewPartnership(
        override val language: String,
        val pack: SponsoringPack,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "new_partnership"

        override fun populate(content: String): String = content
            .replace("{{pack_name}}", pack.name)
            .replace("{{company_name}}", company.name)
    }

    class NewSuggestion(
        override val language: String,
        val pack: SponsoringPack,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "new_suggestion"

        override fun populate(content: String): String = content
            .replace("{{pack_name}}", pack.name)
            .replace("{{company_name}}", company.name)
    }

    data class SuggestionApproved(
        override val language: String,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_approved"

        override fun populate(content: String): String = content
            .replace("{{company_name}}", company.name)
    }

    data class SuggestionDeclined(
        override val language: String,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_declined"

        override fun populate(content: String): String = content
            .replace("{{company_name}}", company.name)
    }

    data class PartnershipValidated(
        override val language: String,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "partnership_validated"

        override fun populate(content: String): String = content
            .replace("{{company_name}}", company.name)
    }

    data class PartnershipDeclined(
        override val language: String,
        val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "partnership_declined"

        override fun populate(content: String): String = content
            .replace("{{company_name}}", company.name)
    }
}
