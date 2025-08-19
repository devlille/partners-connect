package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import java.util.UUID

sealed interface NotificationVariables {
    val usageName: String
    val language: String
    val event: Event
    val company: Company

    fun populate(content: String): String

    class NewPartnership(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val pack: PartnershipPack,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "new_partnership"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    class NewSuggestion(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val pack: PartnershipPack,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "new_suggestion"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class SuggestionApproved(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_approved"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class SuggestionDeclined(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_declined"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipValidated(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val pack: PartnershipPack,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "partnership_validated"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipDeclined(
        override val language: String,
        override val event: Event,
        override val company: Company,
        val eventWithOrganisation: EventWithOrganisation,
        val eventSlug: String,
        val partnershipId: UUID,
    ) : NotificationVariables {
        override val usageName: String = "partnership_declined"

        override fun populate(content: String): String {
            val partnershipLink = "${SystemVarEnv.frontendBaseUrl}/${eventWithOrganisation.organisation.slug}/${eventSlug}/${partnershipId}"
            return content
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipAgreementSigned(
        override val language: String,
        override val event: Event,
        override val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "partnership_agreement_signed"

        override fun populate(content: String): String = content
            .replace("{{company_name}}", company.name)
    }

    data class NewInvoice(
        override val language: String,
        override val event: Event,
        override val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "new_invoice"

        override fun populate(content: String): String = content
            .replace("{{event_name}}", event.name)
            .replace("{{event_contact}}", event.contact.email)
            .replace("{{company_name}}", company.name)
    }

    data class NewQuote(
        override val language: String,
        override val event: Event,
        override val company: Company,
    ) : NotificationVariables {
        override val usageName: String = "new_quote"

        override fun populate(content: String): String = content
            .replace("{{event_name}}", event.name)
            .replace("{{event_contact}}", event.contact.email)
            .replace("{{company_name}}", company.name)
    }
}
