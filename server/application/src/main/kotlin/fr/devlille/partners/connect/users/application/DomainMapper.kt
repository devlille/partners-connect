package fr.devlille.partners.connect.users.application

import fr.devlille.partners.connect.users.domain.User
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity

fun UserEntity.toDomain(): User = User(
    displayName = name,
    pictureUrl = pictureUrl,
    email = email,
)
