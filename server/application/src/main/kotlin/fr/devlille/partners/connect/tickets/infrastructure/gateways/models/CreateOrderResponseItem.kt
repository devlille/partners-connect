package fr.devlille.partners.connect.tickets.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderResponseItem(
    val id: String,
    @SerialName("products_details")
    val productsDetails: List<ProductDetail>,
)

@Serializable
data class ProductDetail(
    val id: String,
    @SerialName("ext_id")
    val extId: String,
    @SerialName("product_download")
    val productDownload: String,
)
