package fr.devlille.partners.connect.auth.infrastructure.api.mappers

import fr.devlille.partners.connect.auth.domain.UserInfo

fun UserInfo.toResponse() = fr.devlille.partners.connect.auth.infrastructure.api.UserInfo(
    displayName = "${this.firstName} ${this.givenName}",
    pictureUrl = this.pictureUrl,
    email = this.email,
)
