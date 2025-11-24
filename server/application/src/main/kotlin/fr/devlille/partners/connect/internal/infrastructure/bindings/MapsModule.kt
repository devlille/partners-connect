package fr.devlille.partners.connect.internal.infrastructure.bindings

import com.google.maps.GeoApiContext
import fr.devlille.partners.connect.internal.infrastructure.geocode.Geocode
import fr.devlille.partners.connect.internal.infrastructure.geocode.GoogleGeocode
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import org.koin.dsl.module

val mapsModule = module {
    single<GeoApiContext> {
        GeoApiContext.Builder()
            .apiKey(SystemVarEnv.GoogleProvider.mapsApiKey)
            .build()
    }
    single<Geocode> {
        GoogleGeocode(geoContext = get())
    }
}
