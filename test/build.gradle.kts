plugins {
    id("java")
    application
}

group = "dev.cwby.jasonify"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":lib"))
    implementation(project(":lib"))
}

application.mainClass.set("dev.cwby.jasonify.Main")
