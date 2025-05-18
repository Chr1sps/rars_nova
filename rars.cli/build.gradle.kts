plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Common dependencies if needed
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }
    }
}