package fr.devlille.partners.connect.auth.infrastructure.providers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class GoogleProvider(private val httpClient: HttpClient) {
    suspend fun getGoogleUserInfo(token: String): GoogleUserInfo = httpClient
        .get("https://openidconnect.googleapis.com/v1/userinfo") {
            headers[HttpHeaders.Authorization] = token
            headers[HttpHeaders.Accept] = "application/json"
        }.body()
}

@Serializable
data class GoogleUserInfo(
    @SerialName("given_name")
    val givenName: String? = null,
    @SerialName("family_name")
    val familyName: String? = null,
    val picture: String? = null,
    val email: String,
)
