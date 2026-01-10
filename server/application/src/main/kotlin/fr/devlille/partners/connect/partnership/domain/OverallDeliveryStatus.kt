package fr.devlille.partners.connect.partnership.domain

/**
 * Overall delivery status sent to a partnership.
 */
enum class OverallDeliveryStatus {
    /** All recipients successfully received */
    SENT,

    /** All recipients failed to receive */
    FAILED,

    /** Some recipients succeeded, some failed */
    PARTIAL,
}
