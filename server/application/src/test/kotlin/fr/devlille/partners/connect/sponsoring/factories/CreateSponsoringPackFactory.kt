package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack

fun createSponsoringPack(
    name: String = "Silver",
    price: Int = 2000,
    nbTickets: Int = 2,
    maxQuantity: Int = 10,
    boothSize: String? = "3x3m",
): CreateSponsoringPack = CreateSponsoringPack(
    name = name,
    price = price,
    nbTickets = nbTickets,
    maxQuantity = maxQuantity,
    boothSize = boothSize,
)
