package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Enhanced sponsoring option with complete translations for organizer management.
 *
 * Extends the basic SponsoringOption structure with translations support, allowing
 * organizers to manage option definitions in multiple languages.
 *
 * Supports four main option types:
 * - text: Traditional text-based sponsoring options
 * - typed_quantitative: Options with quantity-based selection (job offers, articles)
 * - typed_number: Options with fixed quantity selection (booths, banner formats)
 * - typed_selectable: Options with predefined selectable values
 *
 * Uses @JsonClassDiscriminator("type") for polymorphic JSON serialization/deserialization.
 * All subtypes maintain complete translation data for organizer management.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class SponsoringOptionWithTranslations {
    abstract val id: String
    abstract val translations: Map<String, OptionTranslation>
    abstract val price: Int?

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
    ) : SponsoringOptionWithTranslations()

    @Serializable
    @SerialName("typed_quantitative")
    data class TypedQuantitative(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: QuantitativeDescriptor,
    ) : SponsoringOptionWithTranslations()

    @Serializable
    @SerialName("typed_number")
    data class TypedNumber(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int?,
        @SerialName("type_descriptor")
        val typeDescriptor: NumberDescriptor,
        @SerialName("fixed_quantity")
        val fixedQuantity: Int,
    ) : SponsoringOptionWithTranslations()

    @Serializable
    @SerialName("typed_selectable")
    data class TypedSelectable(
        override val id: String,
        override val translations: Map<String, OptionTranslation>,
        override val price: Int? = null,
        @SerialName("type_descriptor")
        val typeDescriptor: SelectableDescriptor,
        @SerialName("selectable_values")
        val selectableValues: List<SelectableValue>,
    ) : SponsoringOptionWithTranslations()
}
