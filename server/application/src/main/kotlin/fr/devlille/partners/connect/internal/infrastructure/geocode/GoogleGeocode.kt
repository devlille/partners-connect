package fr.devlille.partners.connect.internal.infrastructure.geocode

import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.AddressComponentType
import io.ktor.server.plugins.NotFoundException

class GoogleGeocode(
    private val geoContext: GeoApiContext,
) : Geocode {
    override fun countryCode(address: String): String? {
        val address = GeocodingApi.geocode(geoContext, address).await().firstOrNull()
            ?: throw NotFoundException("Address '$address' not found")
        val country = address.addressComponents.find { it.types.contains(AddressComponentType.COUNTRY) }
        return country?.shortName
    }
}
