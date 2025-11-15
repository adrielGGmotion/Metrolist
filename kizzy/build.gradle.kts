plugins {
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.json)
    implementation(libs.kotlin.logging)
    implementation("org.slf4j:slf4j-android:1.7.32")
    testImplementation(libs.junit)
}
