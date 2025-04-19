rootProject.name = "messaging-lib"

include("messaging-core")
include("messaging-quarkus")
include("test-quarkus-messaging")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
