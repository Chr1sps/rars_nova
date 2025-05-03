plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
//    js(IR) {
//        browser()
//    }
//    wasmJs()

    sourceSets {
        val commonMain by getting { }
        val jvmMain by getting { }
    }
}
