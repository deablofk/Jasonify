plugins {
    id("java-library")
}

group = "dev.cwby.jasonify"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.auto.service:auto-service-annotations:1.0")
    annotationProcessor("com.google.auto.service:auto-service:1.0")
    implementation("com.palantir.javapoet:javapoet:0.5.0")
}