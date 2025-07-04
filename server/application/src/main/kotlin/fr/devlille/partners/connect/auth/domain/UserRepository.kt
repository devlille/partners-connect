package fr.devlille.partners.connect.auth.domain

interface UserRepository {
    suspend fun getUserInfo(token: String): UserInfo
}
