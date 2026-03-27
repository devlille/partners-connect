package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.internal.infrastructure.api.FilterDefinition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipListMetadata(
    val filters: List<FilterDefinition>,
    val sorts: List<String>,
    @SerialName("pack_counts")
    val packCounts: List<PackCount>,
)

@Serializable
data class PackCount(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_name")
    val packName: String,
    val count: Int,
)
