plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.shadow)
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
    implementation(libs.bundles.slack.api)
    implementation(libs.bundles.image.processing)
    implementation(libs.google.cloud.storage)
    implementation(libs.flexmark)
    implementation(libs.mustache)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.okhttp.mock)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
