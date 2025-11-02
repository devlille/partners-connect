package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack

fun createSponsoringPack(
    name: String = "Silver",
    price: Int = 2000,
    maxQuantity: Int = 10,
): CreateSponsoringPack = CreateSponsoringPack(
    name = name,
    price = price,
    maxQuantity = maxQuantity,
)
