plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    application
}

application {
    mainClass = "fr.devlille.partners.connect.AppKt"
}

dependencies {
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.kotlinx.ecosystem)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.koin.ktor)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
}
