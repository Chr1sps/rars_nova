plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }
//    js {
//        browser {
//            testTask {
//                useKarma {
//                    useChromeHeadless()
//                }
//            }
//        }
//        nodejs()
//    }
//    val linuxTargets = listOf(
//        linuxArm64(),
//        linuxX64(),
//        mingwX64(),
//        androidNativeX64(),
//        androidNativeX86(),
//        androidNativeArm64(),
//        androidNativeArm32(),
//    )

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
                implementation(kotlin("reflect"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
//        val nativeMain by creating {
//            dependsOn(commonMain)
//        }
//        linuxTargets.forEach {
//            getByName("${it.name}Main") {
//                dependsOn(nativeMain)
//            }
//        }
    }
}
