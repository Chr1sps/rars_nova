plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
application {
    mainClass = "io.github.chr1sps.rars.Main"
    mainModule = "RARSNova"
}

repositories {
    mavenCentral()
}

val graphDoclet by configurations.creating

dependencies {
    implementation("com.formdev:flatlaf:3.4")
    testImplementation("junit:junit:4.13.2")
    graphDoclet("nl.talsmasoftware:umldoclet:2.1.2")
    implementation("org.jetbrains:annotations:24.0.0")
}

tasks.test {
    useJUnitPlatform()
}

group = "io.github.chr1sps"
version = "0.0.1"
description = "RARS Nova"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.javadoc {
    options {
        encoding = "UTF-8"
        docletpath = graphDoclet.files.toList()
        doclet = "nl.talsmasoftware.umldoclet.UMLDoclet"
    }
}

val shadowJar by tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to "io.github.chr1sps.rars.Main"
        )
    }
}


tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.chr1sps.rars.Main"
        )
    }
    from(
        configurations.runtimeClasspath.get().filter { it.isDirectory }
            .map { zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.build {
    dependsOn(shadowJar)
}