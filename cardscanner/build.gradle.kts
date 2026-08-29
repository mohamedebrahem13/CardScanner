import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)

    `maven-publish`
    signing
}

android {
    namespace = "com.cardscanner"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles(
            "consumer-rules.pro"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget =
                JvmTarget.JVM_17
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    // Hilt
    implementation(
        libs.hilt.android
    )

    ksp(
        libs.hilt.compiler
    )

    implementation(
        libs.androidx.hilt.navigation.compose
    )

    // CameraX
    implementation(
        libs.androidx.camera.core
    )

    implementation(
        libs.androidx.camera.camera2
    )

    implementation(
        libs.androidx.camera.lifecycle
    )

    implementation(
        libs.androidx.camera.view
    )

    // Android
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    // ML Kit
    implementation(
        libs.play.services.mlkit.text.recognition
    )

    // Coroutines
    implementation(
        libs.kotlinx.coroutines.android
    )

    implementation(
        libs.kotlinx.coroutines.play.services
    )

    // Compose
    implementation(
        libs.androidx.compose.material.icons.extended
    )

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.foundation
    )

    implementation(
        libs.androidx.compose.material3
    )

    // Lifecycle
    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )
}

afterEvaluate {

    publishing {

        publications {

            create<MavenPublication>(
                "release"
            ) {
                from(
                    components["release"]
                )

                groupId =
                    "io.github.mohamedebrahem13"

                artifactId =
                    "card-scanner"

                version =
                    "1.0.0"

                pom {
                    name.set(
                        "Card Scanner"
                    )

                    description.set(
                        "Android card scanner library using CameraX, Jetpack Compose and ML Kit."
                    )

                    url.set(
                        "https://github.com/mohamedebrahem13/CardScanner"
                    )

                    licenses {
                        license {
                            name.set(
                                "Apache License 2.0"
                            )

                            url.set(
                                "https://www.apache.org/licenses/LICENSE-2.0.txt"
                            )

                            distribution.set(
                                "repo"
                            )
                        }
                    }

                    developers {
                        developer {
                            id.set(
                                "mohamedebrahem13"
                            )

                            name.set(
                                "Mohamed Ebrahem"
                            )

                            url.set(
                                "https://github.com/mohamedebrahem13"
                            )
                        }
                    }

                    scm {
                        connection.set(
                            "scm:git:https://github.com/mohamedebrahem13/CardScanner.git"
                        )

                        developerConnection.set(
                            "scm:git:ssh://git@github.com/mohamedebrahem13/CardScanner.git"
                        )

                        url.set(
                            "https://github.com/mohamedebrahem13/CardScanner"
                        )
                    }
                }
            }
        }

        /*
         * Keep local Maven repo for testing.
         */
        repositories {

            maven {
                name =
                    "localTest"

                url =
                    uri(
                        layout
                            .buildDirectory
                            .dir(
                                "maven-repository"
                            )
                    )
            }
        }
    }

    /*
     * Sign the Maven publication with GPG.
     */
    val publishingExtension =
        extensions.getByType(
            PublishingExtension::class.java
        )

    val releasePublication =
        publishingExtension
            .publications
            .getByName(
                "release"
            )

    val signingExtension =
        extensions.getByType(
            SigningExtension::class.java
        )

    signingExtension.useGpgCmd()

    signingExtension.sign(
        releasePublication
    )
}