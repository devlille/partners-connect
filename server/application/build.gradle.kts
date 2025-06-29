plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass = "fr.devlille.partners.connect.AppKt"
}

dependencies {
    implementation(libs.bundles.kotlinx.ecosystem)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.koin.ktor)
}

tasks.test {
    useJUnitPlatform()
}
