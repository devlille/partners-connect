package fr.devlille.partners.connect.internal.infrastructure.slugify

import java.util.Locale.getDefault

fun String.slugify() = lowercase(getDefault())
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")
    .replace("-+".toRegex(), "-")
