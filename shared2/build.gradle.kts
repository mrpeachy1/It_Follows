plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting

        val androidMain by getting {
            dependencies {
                // This provides colorPrimary, colorOnPrimary, colorPrimaryVariant, etc.
                implementation("com.google.android.material:material:1.12.0")
            }
        }
    }
}


android {
    namespace = "com.itfollows.shared2"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}
