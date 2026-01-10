package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus

/**
 * Provider-agnostic delivery result with metadata.
 *
 * @property overallStatus Computed overall status (SENT if all succeeded, FAILED if all failed, PARTIAL if mixed)
 * @property recipients Per-recipient delivery results
 */
interface DeliveryResult {
    val overallStatus: OverallDeliveryStatus
    val recipients: List<RecipientResult>
}
