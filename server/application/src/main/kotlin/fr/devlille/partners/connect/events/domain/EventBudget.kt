package fr.devlille.partners.connect.events.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventBudget(
    val currency: String,
    val totals: BudgetTotals,
    val packs: List<PackBudget>,
)

@Serializable
data class BudgetTotals(
    val paid: Int,
    val validated: Int,
    @SerialName("validated_minus_paid")
    val validatedMinusPaid: Int,
    val total: Int,
    @SerialName("total_minus_validated")
    val totalMinusValidated: Int,
)

@Serializable
data class PackBudget(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_name")
    val packName: String,
    @SerialName("base_price")
    val basePrice: Int,
    val partnerships: List<PartnershipBudgetItem>,
)

@Serializable
data class PartnershipBudgetItem(
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("price_applied")
    val priceApplied: Int,
)
