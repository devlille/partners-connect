package fr.devlille.partners.connect.auth.infrastructure.api.mappers

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.users.domain.User

fun UserInfo.toDomain(): User = User(
    displayName = "$givenName $firstName",
    email = email,
    pictureUrl = pictureUrl,
)
