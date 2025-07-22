plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    jvm()
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.atomicfu)
            }
        }
        val androidMain by getting {}
        val iosMain by creating {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "io.github.arcadefire.extension"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
}