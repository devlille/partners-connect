package fr.devlille.partners.connect.internal.infrastructure.geocode

interface Geocode {
    fun countryCode(address: String): String?
}
