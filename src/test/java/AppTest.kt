import arrow.core.Either
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import rars.ErrorList
import rars.Globals
import rars.ProgramStatement
import rars.api.Program
import rars.api.ProgramOptions
import rars.events.AssemblyError
import rars.riscv.BasicInstructionFormat
import rars.riscv.InstructionsRegistry
import rars.riscv.hardware.memory.MemoryConfiguration
import rars.riscv.hardware.memory.textSegmentBaseAddress
import rars.settings.BoolSetting
import rars.simulator.Simulator
import rars.util.unwrap
import utils.RarsTestBase
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream

@Suppress("ReplacePrintlnWithLogging")
internal class AppTest : RarsTestBase() {
    // TODO: refactor this class to avoid repetitions and to enhance test speed
    private fun runTest(path: String, is64Bit: Boolean) {
        Globals.BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, is64Bit)
        InstructionsRegistry.RV64_MODE_FLAG = is64Bit

        val (stdin, stdout, stderr, errorLines) = getTestIO(path)
        val programArgs = ProgramOptions().apply {
            startAtMain = true
            maxSteps = 1000
            memoryConfiguration = MemoryConfiguration.DEFAULT
        }
        val program = Program(programArgs)
        doRunFile(path, program, TestIO(stdin, stdout, stderr, errorLines))
    }

    companion object {

        private fun getTestIO(path: String): TestIO {
            var stdin = ""
            var stdout = ""
            var stderr = ""
            val errorLines = mutableSetOf<Int>()
            BufferedReader(FileReader(path)).use { br ->
                var line = br.readLine()
                while (line.startsWith("#")) {
                    if (line.startsWith("#error on lines:")) {
                        val linenumbers = line.replaceFirst("#error on lines:".toRegex(), "").split(",".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                        for (num in linenumbers) {
                            errorLines.add(num.trim { it <= ' ' }.toInt())
                        }
                    } else if (line.startsWith("#stdin:")) {
                        stdin = line.replaceFirst("#stdin:".toRegex(), "").replace("\\\\n".toRegex(), "\n")
                    } else if (line.startsWith("#stdout:")) {
                        stdout = line.replaceFirst("#stdout:".toRegex(), "").replace("\\\\n".toRegex(), "\n")
                    } else if (line.startsWith("#stderr:")) {
                        stderr = line.replaceFirst("#stderr:".toRegex(), "").replace("\\\\n".toRegex(), "\n")
                    }
                    line = br.readLine()
                }
            }
            return TestIO(stdin, stdout, stderr, errorLines)
        }

        @Throws(IOException::class)
        private fun fileProvider(directory: String): Stream<Named<Path?>?>? {
            val path = testDataPath.resolve(directory)
            // noinspection resource
            return Files.walk(path).filter { p: Path? ->
                Files.isRegularFile(p) && p!!.fileName.toString().lowercase(Locale.getDefault()).endsWith(".s")
            }.map<Named<Path?>?> { p: Path? -> Named.of<Path?>(p!!.fileName.toString(), p) }
        }

        @JvmStatic
        fun rv32TestFileProvider() = fileProvider("riscv-tests")

        @JvmStatic
        fun rv64TestFileProvider() = fileProvider("riscv-tests-64")

        @JvmStatic
        fun examplesTestFileProvider() = fileProvider("examples")

        private fun testBasicInstructionBinaryCodesImpl(isRV64Enabled: Boolean) {
            val programArgs = ProgramOptions()
            programArgs.startAtMain = true
            programArgs.maxSteps = 500
            programArgs.selfModifyingCode = true
            programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT
            val program = Program(programArgs)

            Globals.BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true)
            Globals.BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, isRV64Enabled)
            InstructionsRegistry.RV64_MODE_FLAG = isRV64Enabled

            val instructionsToTest = if (isRV64Enabled) InstructionsRegistry.BASIC_INSTRUCTIONS.r64All
            else InstructionsRegistry.BASIC_INSTRUCTIONS.r32All
            for (instruction in instructionsToTest) {
                println("Testing: ${instruction.mnemonic}")
                when (instruction.instructionFormat) {
                    BasicInstructionFormat.B_FORMAT, BasicInstructionFormat.J_FORMAT -> continue
                    else -> Unit
                }
                val format = instruction.exampleFormat

                program.assembleString(format)
                program.setup(emptyList(), "")
                val instructionAddress = MemoryConfiguration.DEFAULT.textSegmentBaseAddress
                val word = program.memory.getWord(instructionAddress).unwrap()

                val baseStatement = program.machineList.first()
                val statementFromMemory = ProgramStatement(word, instructionAddress)

                val message = """
                    Expected:  $baseStatement
                    Actual:    $statementFromMemory
                """.trimIndent()
                println(message)
                assertNotNull(statementFromMemory.instruction, message)
                assertThat(
                    statementFromMemory.printableBasicAssemblyStatement,
                    CoreMatchers.not(CoreMatchers.containsString("invalid"))
                )
                program.assembleString(format)
                program.setup(emptyList(), "")
                val word2 = program.memory.getWord(instructionAddress).unwrap()
                assertEquals(word, word2, "Error 3 on: $format")
                assertEquals(instruction, statementFromMemory.instruction, "Error 4 on: $format")
            }
        }

        private fun testPseudoInstructionsImpl(isRV64: Boolean) {
            val programArgs = ProgramOptions()
            programArgs.startAtMain = true
            programArgs.maxSteps = 500
            programArgs.selfModifyingCode = true
            programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT
            val program = Program(programArgs)
            Globals.BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true)
            Globals.BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, isRV64)
            InstructionsRegistry.RV64_MODE_FLAG = isRV64

            val instructionsToTest = if (isRV64) InstructionsRegistry.EXTENDED_INSTRUCTIONS.r64All
            else InstructionsRegistry.EXTENDED_INSTRUCTIONS.r32All
            for (instruction in instructionsToTest) {
                val programString = "label:" + instruction.exampleFormat
                program.assembleString(programString)
                program.setup(emptyList(), "")
                val first = program.memory.getWord(0x400000).unwrap()
                val second = program.memory.getWord(0x400004).unwrap()
                val ps = ProgramStatement(first, 0x400000)
                assertNotNull(ps.instruction, "Error 11 on: $programString")
                assertThat(
                    "Error 12 on: $programString",
                    ps.printableBasicAssemblyStatement,
                    CoreMatchers.not(CoreMatchers.containsString("invalid"))
                )
                if ("t0" in programString || "t1" in programString || "t2" in programString || "f1" in programString) {
                    // TODO: test that each register individually is meaningful and test every
                    // register.
                    // Currently this covers all instructions and is an alert if I made a trivial
                    // mistake.
                    val registerSubstitute =
                        programString.replace("t0|t1|t2".toRegex(), "x0").replace("f1".toRegex(), "f0")
                    program.assembleString(registerSubstitute)
                    program.setup(emptyList(), "")
                    val word1 = program.memory.getWord(0x400000).unwrap()
                    val word2 = program.memory.getWord(0x400004).unwrap()
                    Assertions.assertFalse(word1 == first && word2 == second, "Error 13 on: $programString")
                }
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun runSingle() {
        val path = "riscv-tests/fclass.s"
        runTest(testDataPath.resolve(path).toString(), false)
    }

    @Test
    fun testBasicInstructionBinaryCodes32() = testBasicInstructionBinaryCodesImpl(false)

    @Test
    fun testBasicInstructionBinaryCodes64() = testBasicInstructionBinaryCodesImpl(true)

    @Test
    fun testPseudoInstructions32() = testPseudoInstructionsImpl(false)

    @Test
    fun testPseudoInstructions64() = testPseudoInstructionsImpl(true)

    @DisplayName("32 bit instructions")
    @ParameterizedTest
    @MethodSource("rv32TestFileProvider")
    @Throws(IOException::class)
    fun test32(path: Path) = runTest(path.toString(), false)

    @DisplayName("64 bit instructions")
    @ParameterizedTest
    @MethodSource("rv64TestFileProvider")
    @Throws(IOException::class)
    fun test64(path: Path) = runTest(path.toString(), true)

    @DisplayName("Examples")
    @ParameterizedTest
    @MethodSource("examplesTestFileProvider")
    @Throws(IOException::class)
    fun testExamples(path: Path) = runTest(path.toString(), false)
}

data class TestIO(
    val stdin: String,
    val stdout: String,
    val stderr: String,
    val errorLines: Set<Int>
) {
    constructor() : this("", "", "", emptySet())
}

@Suppress("ReplacePrintlnWithLogging")
private fun RarsTestBase.doRunImpl(
    program: Program,
    testIO: TestIO,
    assemblyFunc: Program.() -> Either<AssemblyError, ErrorList>
) {
    val (stdin, stdout, stderr, errorLines) = testIO
    program.assemblyFunc().onLeft { assemblyError ->
        if (errorLines.isEmpty()) {
            fail {
                buildString {
                    append("Failed to assemble `$testName` due to following error(s):\n")
                    for (error in assemblyError.errors.errorMessages) {
                        append("[${error.lineNumber},${error.position}] ${error.generateReport()}\n")
                    }
                }
            }
        }
        val errors = assemblyError.errors.errorMessages
        val foundErrorLines = buildSet {
            for (error in errors) {
                if (error.isWarning) continue
                add(error.lineNumber)
            }
        }
        if (errorLines != foundErrorLines) {
            fail {
                buildString {
                    append(
                        """
                                Expected and actual error lines are not equal for `$testName`.
                                Expected lines: $errorLines
                                Errors found:
                                """.trimIndent()
                    )
                    for (error in errors) {
                        append("[${error.lineNumber},${error.position}] ${error.generateReport()}\n")
                    }
                }
            }
        }
        return
    }
    if (!errorLines.isEmpty()) {
        fail { "Expected assembly error, but successfully assembled `$testName`.\n" }
    }
    program.setup(emptyList(), stdin)
    println("Machine list:")
    program.machineList.forEach { x -> println(x) }
    println()
    program.simulate().fold(
        { error ->
            fail {
                """
                    Crashed while executing `$testName`.
                    Reason: ${error.reason}.
                    Value: ${error.value}.
                    Message:
                    ```
                    ${error.message.generateReport()}
                    ```
                    """.trimIndent()
            }
        },
        { reason ->
            when {
                reason != Simulator.Reason.NORMAL_TERMINATION -> fail {
                    """
                    Ended abnormally while executing `$testName`.
                    Reason: $reason.
                    """.trimIndent()
                }
                program.exitCode != 42 -> fail {
                    """
                    Final exit code was wrong for `$testName`.
                    Expected: 42, but got ${program.exitCode}.
                    """.trimIndent()
                }
                program.stdout != stdout -> fail {
                    """
                    STDOUT was wrong for `$testName`.
                    Expected:
                    \"$stdout\",
                    but got
                    \"${program.stdout}\".
                    """.trimIndent()
                }
                program.stderr != stderr -> fail {
                    """
                    STDERR was wrong for `$testName`.
                    Expected:
                    \"$stderr\",
                    but got
                    \"${program.stderr}\".
                    """.trimIndent()
                }
                else -> Unit
            }
        }
    )
}

fun RarsTestBase.doRunFile(path: String, program: Program, testIO: TestIO) =
    doRunImpl(program, testIO) { assembleFile(File(path)) }

fun RarsTestBase.doRunString(code: String, program: Program, testIO: TestIO = TestIO()) =
    doRunImpl(program, testIO) { assembleString(code) }