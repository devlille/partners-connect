package fr.devlille.partners.connect.auth.application

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.auth.domain.UserRepository
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider

class GoogleUserRepository(
    private val googleProvider: GoogleProvider
) : UserRepository {
    override suspend fun getUserInfo(token: String): UserInfo {
        val googleUserInfo = googleProvider.getGoogleUserInfo(token)
        return UserInfo(
            givenName = googleUserInfo.givenName,
            firstName = googleUserInfo.familyName,
            pictureUrl = googleUserInfo.picture,
            email = googleUserInfo.email
        )
    }
}
