plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()

    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val androidMain by getting
        val androidUnitTest by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosTest by creating {
            dependsOn(commonTest)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.itfollows.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}
