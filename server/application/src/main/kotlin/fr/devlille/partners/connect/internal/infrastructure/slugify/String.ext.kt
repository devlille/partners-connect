package fr.devlille.partners.connect.internal.infrastructure.slugify

import java.util.Locale.getDefault

fun String.slugify() = lowercase(getDefault())
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")

fun String.slugify(
    maxLength: Int = 64,
    defaultSlug: String = "n-a",
): String {
    val slug = lowercase(getDefault())
        .replace("\n", " ")
        .replace("[^a-z\\d\\s]".toRegex(), " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString("-")
        .replace("-+".toRegex(), "-")
        .trim('-')
    val finalSlug = if (slug.isEmpty()) defaultSlug else slug.take(maxLength)
    return finalSlug
}
