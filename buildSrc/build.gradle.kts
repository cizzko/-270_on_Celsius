plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
}
