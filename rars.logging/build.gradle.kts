plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }
//        val jvmMain by getting { }
//        val wasmJsMain by getting {}
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}
