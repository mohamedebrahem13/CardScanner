import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
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

    /*
     * ============================================
     * MAVEN PUBLICATION
     * ============================================
     *
     * Publish only the release AAR.
     * Also generate sources.jar.
     */
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

}

dependencies {
    /*
     * Hilt
     */
    implementation(
        libs.hilt.android
    )

    ksp(
        libs.hilt.compiler
    )

    implementation(
        libs.androidx.hilt.navigation.compose
    )

    /*
     * CameraX
     */
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

    /*
     * Android
     */
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    /*
     * ML Kit
     */
    implementation(
        libs.play.services.mlkit.text.recognition
    )

    /*
     * Coroutines
     */
    implementation(
        libs.kotlinx.coroutines.android
    )

    implementation(
        libs.kotlinx.coroutines.play.services
    )

    /*
     * Compose
     */
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

    /*
     * Lifecycle
     */
    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )
}

/*
 * ================================================
 * MAVEN PUBLISHING
 * ================================================
 *
 * This currently publishes to a LOCAL Maven folder
 * for testing.
 *
 * Generated dependency:
 *
 * com.cardscanner:card-scanner:1.0.0
 *
 * We will change groupId later when publishing to
 * Maven Central using your verified namespace.
 * ================================================
 */

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
                            "scm:git:git://github.com/mohamedebrahem13/CardScanner.git"
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

        repositories {

            /*
             * Keep this only for testing.
             */
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

    extensions
        .getByType(
            SigningExtension::class.java
        )
        .sign(
            releasePublication
        )
}