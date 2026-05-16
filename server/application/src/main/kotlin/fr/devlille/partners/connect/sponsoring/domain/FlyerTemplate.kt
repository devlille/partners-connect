package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
data class FlyerZone(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class FlyerTemplate(
    val templateUrl: String,
    val zone: FlyerZone,
)
