package fr.devlille.partners.connect.ecosystempartners.domain

import java.util.UUID

/**
 * Lifecycle events that trigger a notification for an ecosystem partner.
 * Each value maps to a notification template directory under
 * `notifications/{email|slack}/ecosystem_partner_<templateName>/`.
 */
enum class EcosystemPartnerLifecycleEvent(val templateName: String) {
    SUBMITTED("ecosystem_partner_submitted"),
    VALIDATED("ecosystem_partner_validated"),
    DECLINED("ecosystem_partner_declined"),
    REMOVED("ecosystem_partner_removed"),
}

/**
 * Dispatches ecosystem partner lifecycle notifications through the
 * configured notification integrations for an event (Mailjet, Slack).
 *
 * This is intentionally separate from the partnership-centric
 * `NotificationRepository.sendMessage(variables)` pipeline: ecosystem
 * partners do not have a classic `PartnershipEntity`, and re-using the
 * Mailjet gateway's partnership lookup would either crash or require a
 * compromised abstraction. Treating ecosystem partner notifications as
 * their own domain concern keeps both pipelines focused.
 */
interface EcosystemPartnerNotificationRepository {
    suspend fun notify(
        eventSlug: String,
        ecosystemPartnerId: UUID,
        lifecycle: EcosystemPartnerLifecycleEvent,
    )
}
