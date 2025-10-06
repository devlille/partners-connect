import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.shadow)
    application
}

val mainKlass = "fr.devlille.partners.connect.AppKt"
application {
    this.mainClass = mainKlass
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = mainKlass
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
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
    implementation(libs.json.schema.validator)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.okhttp.mock)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
