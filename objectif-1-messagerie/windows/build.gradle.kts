plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.2"
    application
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
    compilerOptions { freeCompilerArgs.add("-Xcontext-receivers") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation("co.tamboai:TModeler:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

application { mainClass.set("ai.tambo.foundation.ngoma.MainKt") }
