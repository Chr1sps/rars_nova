plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
application {
    mainClass = "rars.Main"
//    mainModule = "RARSNova"
}
repositories {
    mavenCentral()
}

val graphDoclet: Configuration by configurations.creating

dependencies {
    implementation("com.formdev:flatlaf:3.4")
    compileOnly("org.jetbrains:annotations:24.0.0")
    testCompileOnly("org.jetbrains:annotations:24.0.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    graphDoclet("nl.talsmasoftware:umldoclet:2.2.0")
}


group = "io.github.chr1sps"
version = "0.0.1"
description = "RARS Nova"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21


val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to "rars.Main",
            "Multi-Release" to "true"
        )
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    jar {
        manifest {
            attributes(
                "Main-Class" to "rars.Main"
            )
        }
        from(
            configurations.runtimeClasspath.get().filter { it.isDirectory }
                .map { zipTree(it) }
        )
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    build {
        dependsOn(shadowJar)
    }
    test {
        useJUnitPlatform()
    }
    javadoc {
        val docletOptions = options as StandardJavadocDocletOptions
        docletOptions.apply {
            encoding = "UTF-8"
            docletpath = graphDoclet.files.toList()
//            doclet = "nl.talsmasoftware.umldoclet.UMLDoclet"
        }
    }
}