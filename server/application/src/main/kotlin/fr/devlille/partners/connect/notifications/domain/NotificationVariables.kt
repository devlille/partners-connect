package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.digest.domain.DigestEntry
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.domain.Partnership
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import fr.devlille.partners.connect.partnership.domain.link

sealed interface NotificationVariables {
    val usageName: String
    val language: String
    val event: EventWithOrganisation
    val company: Company

    fun populate(content: String): String

    class NewPartnership(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val pack: PartnershipPack,
    ) : NotificationVariables {
        override val usageName: String = "new_partnership"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    class NewSuggestion(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val pack: PartnershipPack,
    ) : NotificationVariables {
        override val usageName: String = "new_suggestion"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class SuggestionApproved(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_approved"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class SuggestionDeclined(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
    ) : NotificationVariables {
        override val usageName: String = "suggestion_declined"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipValidated(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val pack: PartnershipPack,
    ) : NotificationVariables {
        override val usageName: String = "partnership_validated"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{pack_name}}", pack.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipDeclined(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
    ) : NotificationVariables {
        override val usageName: String = "partnership_declined"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class BillingStatusChanged(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val status: InvoiceStatus,
    ) : NotificationVariables {
        override val usageName: String = "billing_status_changed"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
                .replace("{{billing_status}}", status.name.lowercase())
        }
    }

    data class PartnershipAgreement(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: PartnershipDetail,
    ) : NotificationVariables {
        override val usageName: String = "partnership_agreement"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class PartnershipAgreementSigned(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
    ) : NotificationVariables {
        override val usageName: String = "partnership_agreement_signed"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class NewInvoice(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: PartnershipDetail,
    ) : NotificationVariables {
        override val usageName: String = "new_invoice"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class NewQuote(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: PartnershipDetail,
    ) : NotificationVariables {
        override val usageName: String = "new_quote"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class JobOfferApproved(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val jobOffer: JobOffer,
    ) : NotificationVariables {
        override val usageName: String = "job_offer_approved"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{job_title}}", jobOffer.title)
                .replace("{{job_offer_url}}", jobOffer.url)
                .replace("{{partnership_link}}", partnershipLink)
        }
    }

    data class JobOfferDeclined(
        override val language: String,
        override val event: EventWithOrganisation,
        override val company: Company,
        val partnership: Partnership,
        val jobOffer: JobOffer,
        val declineReason: String?,
    ) : NotificationVariables {
        override val usageName: String = "job_offer_declined"

        override fun populate(content: String): String {
            val partnershipLink = partnership.link(event)
            val reasonText = declineReason ?: "No reason provided"
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{event_contact}}", event.event.contact.email)
                .replace("{{company_name}}", company.name)
                .replace("{{job_title}}", jobOffer.title)
                .replace("{{job_offer_url}}", jobOffer.url)
                .replace("{{partnership_link}}", partnershipLink)
                .replace("{{decline_reason}}", reasonText)
        }
    }

    /**
     * Notification variables for the morning organiser daily digest.
     *
     * Populates the `digest/{language}.md` template with up to three sections.
     * The [company] property is not applicable for a digest (digest covers multiple
     * companies) and throws [UnsupportedOperationException] if accessed.
     */
    class MorningDigest(
        override val language: String,
        override val event: EventWithOrganisation,
        val agreementItems: List<DigestEntry>,
        val billingItems: List<DigestEntry>,
        val socialMediaItems: List<DigestEntry>,
        val jobOfferItems: List<DigestEntry>,
    ) : NotificationVariables {
        override val usageName: String = "digest"

        override val company: Company
            get() = error("Not applicable for MorningDigest")

        private fun formatSection(items: List<DigestEntry>, emptyMessage: String): String =
            if (items.isEmpty()) {
                emptyMessage
            } else {
                items.joinToString("\n") { "• <${it.partnershipLink}|${it.companyName}>" }
            }

        override fun populate(content: String): String {
            return content
                .replace("{{event_name}}", event.event.name)
                .replace("{{agreement_section}}", formatSection(agreementItems, "n/a"))
                .replace("{{billing_section}}", formatSection(billingItems, "n/a"))
                .replace("{{social_media_section}}", formatSection(socialMediaItems, "n/a"))
                .replace("{{job_offer_section}}", formatSection(jobOfferItems, "n/a"))
        }
    }
}
