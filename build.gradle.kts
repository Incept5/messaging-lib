// Set default version and group
group = "com.github.incept5"  // Default group for local development

// Determine the version to use
val providedVersion = project.properties["version"]?.toString()
val buildNumber = project.properties["buildNumber"]?.toString()

// Set the version based on the available information
if (providedVersion != null && providedVersion != "unspecified" && providedVersion != "1.0.0-SNAPSHOT") {
    version = providedVersion
} else if (buildNumber != null && buildNumber.isNotEmpty()) {
    version = "1.0.$buildNumber"
} else {
    version = "1.0.0-SNAPSHOT"
}

// If a specific group is provided, use that
val providedGroup = project.properties["group"]?.toString()
if (providedGroup != null && providedGroup.isNotEmpty()) {
    group = providedGroup
}

// Check for publishGroupId which will be used for Maven publications
val publishGroupId = project.properties["publishGroupId"]?.toString() ?: group.toString()

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // publish to maven repositories
    `maven-publish`
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Configure Kotlin to target JVM 21
kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Apply common configuration to all subprojects
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    // Get the publishGroupId from root project
    val publishGroupId = rootProject.properties["publishGroupId"]?.toString() ?: rootProject.group.toString()

    java {
        withJavadocJar()
        withSourcesJar()
    }

    // Configure Kotlin to target JVM 21
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // Configure publishing for all subprojects
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }

        // Ensure all publications use the correct group and version
        publications.withType<MavenPublication>().configureEach {
            // For JitPack compatibility, we need to use the correct group ID format
            // JitPack expects: com.github.{username}.{repository}
            val jitpackGroupId = if (System.getenv("JITPACK") != null) {
                // When building on JitPack
                "com.github.incept5.messaging-lib"
            } else {
                // For local development
                publishGroupId
            }

            // Use publishGroupId instead of project.group to ensure correct artifact path
            groupId = jitpackGroupId
            version = project.version.toString()

            // Add SCM information for JitPack
            pom {
                scm {
                    connection.set("scm:git:github.com/incept5/messaging-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/incept5/messaging-lib.git")
                    url.set("https://github.com/incept5/messaging-lib/tree/main")
                }
            }
        }
    }
}

// Add a task to publish only the modules we want to JitPack
tasks.register("publishJitPackModules") {
    dependsOn(":messaging-core:publishToMavenLocal", ":messaging-quarkus:publishToMavenLocal")
}

// Configure root project publishing for JitPack
publishing {
    publications {
        create<MavenPublication>("mavenRoot") {
            // For JitPack compatibility
            groupId = "com.github.incept5"
            artifactId = "messaging-lib"
            version = project.version.toString()

            // Create an empty JAR for the root project
            artifact(tasks.register("emptyJar", Jar::class) {
                archiveClassifier.set("empty")
            })

            // POM information
            pom {
                name.set("messaging Library")
                description.set("messaging  library for Quarkus applications")
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

                scm {
                    connection.set("scm:git:github.com/incept5/messaging-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/incept5/messaging-lib.git")
                    url.set("https://github.com/incept5/messaging-lib/tree/main")
                }

                // Define dependencies on the subprojects
                withXml {
                    val dependencies = asNode().appendNode("dependencies")

                    project(":messaging-core").let { _ ->
                        val dependency = dependencies.appendNode("dependency")
                        dependency.appendNode("groupId", "com.github.incept5.messaging-lib")
                        dependency.appendNode("artifactId", "messaging-core")
                        dependency.appendNode("version", project.version)
                        dependency.appendNode("scope", "compile")
                    }

                    project(":messaging-quarkus").let { _ ->
                        val dependency = dependencies.appendNode("dependency")
                        dependency.appendNode("groupId", "com.github.incept5.messaging-lib")
                        dependency.appendNode("artifactId", "messaging-quarkus")
                        dependency.appendNode("version", project.version)
                        dependency.appendNode("scope", "compile")
                    }

                    project(":test-quarkus-messaging").let { _ ->
                        val dependency = dependencies.appendNode("dependency")
                        dependency.appendNode("groupId", "com.github.incept5.messaging-lib")
                        dependency.appendNode("artifactId", "test-quarkus-messaging")
                        dependency.appendNode("version", project.version)
                        dependency.appendNode("scope", "compile")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}


