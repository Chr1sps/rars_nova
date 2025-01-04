import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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
    implementation("com.fifesoft:rsyntaxtextarea:3.5.2")
    implementation("de.jflex:jflex:1.9.1")
    implementation("info.picocli:picocli:4.7.6")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    graphDoclet("nl.talsmasoftware:umldoclet:2.2.0")
}


group = "io.github.chr1sps"
version = "0.0.1"
description = "RARS Nova"


tasks.getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
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

sourceSets.main {
    java.srcDirs("src/main/java", "src/generated/java")
}

// region JFlex

// region Utils
fun String.findMethod(header: String, vararg requiredContents: String): IntRange? {
    // First, find the method header index
    val headerIndex = indexOf(header).takeIf { it != -1 } ?: return null

    // Next, find the method end index by looking for the end brace
    val firstLBraceIndex = indexOf('{', startIndex = headerIndex)
    var braceCount = 0
    val endBraceIndex = subSequence(firstLBraceIndex, length).indexOfFirst {
        when (it) {
            '{' -> braceCount++
            '}' -> braceCount--
        }
        braceCount == 0
    }
    val adjustedEndBraceIndex = firstLBraceIndex + endBraceIndex

    // Last, find the preceding docs start
    // This must be the last occurrence of "/**" before the header
    val docsStartIndex = subSequence(0, headerIndex).lastIndexOf("/**")

    subSequence(docsStartIndex, adjustedEndBraceIndex).let { methodContent ->
        if (requiredContents.any { it !in methodContent }) {
            return null
        }
    }

    // Now, we must find the preceding newline character for the docs
    // and the following newline character for the method
    val docsNewlineIndex = subSequence(0, docsStartIndex).lastIndexOf('\n')
    val methodNewlineIndex = indexOf('\n', startIndex = adjustedEndBraceIndex)

    return docsNewlineIndex + 1 until methodNewlineIndex
}

/**
 * Finds the range of the zzRefill method in the lexer file.
 * The range encompasses the entire method, including the preceding docs.
 */
fun String.findZZRefill(): IntRange? = findMethod("private boolean zzRefill() throws java.io.IOException ")

fun String.findYYReset(): IntRange? =
    findMethod("public final void yyreset(java.io.Reader reader)", "zzBuffer = new char[initBufferSize];")
// endregion Utils

// region Paths
val buildPath = layout.buildDirectory.asFile.get().absolutePath

val lexerClassName = "RVLexer"
val lexerFlexDir = "src/main/resources"
val lexerOutputDir = "src/generated/java/rars/riscv/lang/lexing"
val flexFileName = "$lexerFlexDir/$lexerClassName.flex"
val customScriptsOutputDir = "$buildPath/customScripts"
val flexCacheFileName = "$customScriptsOutputDir/$lexerClassName.cache.flex"
val lexerOutputName = "$lexerOutputDir/$lexerClassName.java"
// endregion Paths

val cacheJFlexFile by tasks.register<CacheFileTask>("cacheJFlexFile") {
    inputFile.set(file(flexFileName))
    outputFile.set(file(flexCacheFileName))
}

val runJFlex = tasks.register<JavaExec>("runJFlex") {
    withCache(cacheJFlexFile)
    description = "Generates a lexer"
    mainClass = "jflex.Main"
    classpath = sourceSets["main"].compileClasspath
    inputs.file(flexCacheFileName)
    outputs.file(lexerOutputName)
    args = listOf(
        "-d", lexerOutputDir,
        flexFileName
    )
    doLast {
        println("Generated lexer")
    }
}

val removeDuplicateLexerMethods = tasks.register("removeDuplicateLexerMethods") {
    group = "build"
    description = "Modifies the generated lexer to remove specific methods."

    dependsOn(runJFlex)
    withCache(cacheJFlexFile)

    inputs.file(lexerOutputName)
    outputs.file(lexerOutputName)

    val lexerFile = file("$lexerOutputDir/$lexerClassName.java")

    doLast {
        if (lexerFile.exists()) {
            val content = lexerFile.readText()

            // Remove both methods from the content
            fun String.doRemove(range: IntRange): String {
                println("Found method:")
                println(subSequence(range))
                return removeRange(range)
            }

            var isModified = false
            val modifiedContent = content
                .run {
                    val zzRefillRange = findZZRefill()
                    if (zzRefillRange != null) {
                        isModified = true
                        doRemove(zzRefillRange)
                    } else {
                        println("zzRefill method not found, skipping removal.")
                        this
                    }
                }
                .run {
                    val yyResetRange = findYYReset()
                    if (yyResetRange != null) {
                        isModified = true
                        doRemove(yyResetRange)
                    } else {
                        println("yyreset method not found, skipping removal.")
                        this
                    }
                }

            if (isModified) {
                lexerFile.writeText(modifiedContent)
                println("Lexer modification completed.")
            } else {
                println("No methods found to remove.")
            }
        } else {
            println("Lexer file not found, skipping modification.")
        }
    }
}

val createLexer = tasks.register("createLexer") {
    group = "build"
    description = "Generates the lexer and removes specific methods."
    dependsOn(removeDuplicateLexerMethods)
    withCache(cacheJFlexFile)
    doLast {
        val backupFile = file("$lexerOutputDir/$lexerClassName.java~")
        if (backupFile.exists()) {
            backupFile.delete()
        }
    }
}

tasks.named("compileJava") {
    dependsOn(createLexer)
}
// endregion JFlex
abstract class CacheFileTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val didUpdate: Property<Boolean>

    init {
        didUpdate.convention(false)
    }

    @TaskAction
    fun updateAndCache() {
        val inputContent = inputFile.get().asFile.readText()
        val cachedFile = outputFile.get().asFile

        didUpdate = if (!cachedFile.exists() || cachedFile.readText() != inputContent) {
            logger.lifecycle("Updating cached file: $cachedFile")
            cachedFile.parentFile.mkdirs()
            cachedFile.writeText(inputContent)
            true
        } else {
            logger.lifecycle("Cached file is up-to-date: $cachedFile")
            false
        }
    }
}

fun <T : Task> T.withCache(cacheTask: CacheFileTask): T {
    dependsOn(cacheTask)
    onlyIf {
        cacheTask.didUpdate.get()
    }
    return this
}