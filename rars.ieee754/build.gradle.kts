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
        commonMain
        jvmMain
    }
}
