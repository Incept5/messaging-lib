plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    // publish to nexus
    `maven-publish`
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.incept5.correlation)
    implementation(libs.incept5.json)
    api(libs.jackson.annotations)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.hsqldb:hsqldb:2.7.2")
    testRuntimeOnly(libs.slf4j.simple) // slf4j backend
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // For JitPack compatibility, we need to use the correct group ID format
            // JitPack expects: com.github.{username}.{repository}
            val publishGroupId = rootProject.properties["publishGroupId"]?.toString()
                ?: if (System.getenv("JITPACK") != null) {
                    // When building on JitPack
                    "com.github.incept5.messaging-lib"
                } else {
                    // For local development
                    "com.github.incept5"
                }

            // Explicitly set the coordinates
            groupId = publishGroupId
            artifactId = "messaging-core"
            version = project.version.toString()

            from(components["java"])

            // POM information
            pom {
                name.set("Authz Core")
                description.set("Core functionality for messaging in Rest Services")
                url.set("https://github.com/incept5/messaging-lib")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("incept5")
                        name.set("Incept5")
                        email.set("info@incept5.com")
                    }
                }

                // Important for JitPack to resolve dependencies correctly
                scm {
                    connection.set("scm:git:github.com/incept5/messaging-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/incept5/messaging-lib.git")
                    url.set("https://github.com/incept5/messaging-lib/tree/main")
                }
            }
        }
    }
}

// For JitPack compatibility
tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

// Always publish to local Maven repository after build for local development
tasks.named("build") {
    finalizedBy(tasks.named("publishToMavenLocal"))
}