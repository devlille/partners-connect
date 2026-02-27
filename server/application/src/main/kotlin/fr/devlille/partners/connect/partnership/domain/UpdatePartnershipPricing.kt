package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for updating price overrides on a partnership.
 *
 * Partial update semantics:
 * - [packPriceOverride] = null → clear any existing pack override (reverts to catalogue price)
 * - [packPriceOverride] = integer → set/replace override
 * - [optionsPriceOverrides] absent (null) → all option overrides unchanged
 * - [optionsPriceOverrides] present → only listed option IDs are affected; others unchanged
 */
@Serializable
data class UpdatePartnershipPricing(
    @SerialName("pack_price_override")
    val packPriceOverride: Int? = null,
    @SerialName("options_price_overrides")
    val optionsPriceOverrides: List<OptionPriceOverride>? = null,
)

/**
 * Per-option price override entry used in [UpdatePartnershipPricing].
 *
 * - [id] must be the UUID of an option already associated with the partnership.
 * - [priceOverride] = null clears any existing override (revert to catalogue price).
 * - [priceOverride] = integer sets or replaces the override.
 */
@Serializable
data class OptionPriceOverride(
    val id: String,
    @SerialName("price_override")
    val priceOverride: Int? = null,
)
