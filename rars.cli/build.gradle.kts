plugins {
    kotlin("multiplatform")
}

//dependencies {
//    testImplementation(kotlin("test"))
//}
//
//tasks.test {
//    useJUnitPlatform()
//}
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