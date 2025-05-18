plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
    wasmJs()

    sourceSets {
        commonMain {
            dependencies {
                api("io.arrow-kt:arrow-core:2.0.1")
                implementation(project(":rars.logging"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
