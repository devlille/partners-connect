package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enum defining the types of sponsoring options available.
 * Used to discriminate between different option variants in the polymorphic sealed class.
 */
@Serializable
enum class OptionType {
    @SerialName("text")
    TEXT,

    @SerialName("typed_quantitative")
    TYPED_QUANTITATIVE,

    @SerialName("typed_number")
    TYPED_NUMBER,

    @SerialName("typed_selectable")
    TYPED_SELECTABLE,
}

/**
 * Type descriptors for quantitative options where partners select quantities.
 * Provides semantic meaning to the quantity selection (e.g., "job offers").
 */
@Serializable
enum class QuantitativeDescriptor {
    @SerialName("job_offer")
    JOB_OFFER,
}

/**
 * Type descriptors for number options with fixed quantities set by organizers.
 * Provides semantic meaning to the fixed quantity (e.g., "tickets").
 */
@Serializable
enum class NumberDescriptor {
    @SerialName("nb_ticket")
    NB_TICKET,
}

/**
 * Type descriptors for selectable options where partners choose from predefined values.
 * Provides semantic meaning to the selectable values (e.g., "booth size").
 */
@Serializable
enum class SelectableDescriptor {
    @SerialName("booth")
    BOOTH,
}
