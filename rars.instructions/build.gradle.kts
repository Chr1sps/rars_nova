plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
    wasmJs()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.arrow-kt:arrow-core:2.0.1")
            }
        }
    }
}
