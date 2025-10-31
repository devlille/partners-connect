package fr.devlille.partners.connect.companies.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the lifecycle state of a company.
 * Used for soft deletion and status filtering.
 */
@Serializable
enum class CompanyStatus {
    /**
     * Normal operational company (default for existing records).
     */
    @SerialName("active")
    ACTIVE,

    /**
     * Soft-deleted company (preserved for historical integrity).
     */
    @SerialName("inactive")
    INACTIVE,
}
