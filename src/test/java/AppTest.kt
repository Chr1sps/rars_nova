import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import rars.Globals
import rars.ProgramStatement
import rars.api.Program
import rars.api.ProgramOptions
import rars.exceptions.AddressErrorException
import rars.exceptions.SimulationException
import rars.riscv.BasicInstructionFormat
import rars.riscv.InstructionsRegistry
import rars.riscv.hardware.MemoryConfiguration
import rars.settings.BoolSetting
import rars.simulator.Simulator
import utils.RarsTestBase
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

@Suppress("ReplacePrintlnWithLogging")
internal class AppTest : RarsTestBase() {
    companion object {

        // TODO: refactor this class to avoid repetitions and to enhance test speed
        @Throws(IOException::class)
        private fun runTest(path: String, is64Bit: Boolean) {
            Globals.BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, is64Bit)
            InstructionsRegistry.RV64_MODE_FLAG = is64Bit

            val errorLines = HashSet<Int?>()
            var stdin = ""
            var stdout = ""
            var stderr = ""
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
            val programArgs = ProgramOptions()
            programArgs.startAtMain = true
            programArgs.maxSteps = 1000
            programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT
            val program = Program(programArgs)
            try {
                program.assembleFile(File(path)).onLeft { assemblyError ->
                    if (errorLines.isEmpty()) {
                        val builder = StringBuilder()
                        builder.append("Failed to assemble `$path` due to following error(s):\n")
                        for (error in assemblyError.errors.errorMessages) {
                            builder.append(error.generateReport()).append("\n")
                        }
                        Assertions.fail<Any?>(builder.toString())
                    }
                    val errors = assemblyError.errors.errorMessages
                    val foundErrorLines = HashSet<Int?>()
                    for (error in errors) {
                        if (error.isWarning) continue
                        foundErrorLines.add(error.lineNumber)
                    }
                    if (errorLines != foundErrorLines) {
                        fail {
                            buildString {
                                append(
                                    """
                                Expected and actual error lines are not equal for `$path`.
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
                    fail { "Expected assembly error, but successfully assembled `$path`.\n" }
                }
                program.setup(mutableListOf<String>(), stdin)
                println("Machine list:")
                program.machineList.forEach(Consumer { x: ProgramStatement? -> println(x) })
                println()
                val r = program.simulate()
                if (r != Simulator.Reason.NORMAL_TERMINATION) {
                    Assertions.fail(
                        """
                            Ended abnormally while executing `$path`.
                            Reason: $r.
                        """.trimIndent()
                    )
                } else {
                    if (program.exitCode != 42) {
                        val msg =
                            "Final exit code was wrong for `" + path + "`.\n" + "Expected: 42, but got " + program.exitCode + "."
                        Assertions.fail<Any?>(msg)
                    }
                    if (program.sTDOUT != stdout) {
                        val msg =
                            "STDOUT was wrong for `" + path + "`.\n" + "Expected:\n\"" + stdout + "\",\nbut got \"" + program.sTDOUT + "\"."
                        Assertions.fail<Any?>(msg)
                    }
                    if (program.sTDERR != stderr) {
                        val msg =
                            "STDERR was wrong for `" + path + "`.\n" + "Expected:\n\"" + stderr + "\",\nbut got \"" + program.sTDERR + "\"."
                        Assertions.fail<Any?>(msg)
                    }
                }
            } catch (se: SimulationException) {
                fail {
                    """
                    Crashed while executing `$path`.
                    Reason: ${se.reason}.
                    Value: ${se.value}.
                    Message:
                    ```
                    ${se.errorMessage!!.generateReport()}
                    ```
                    """.trimIndent()
                }
            }
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

        private fun testBasicInstructionBinaryCodesImpl(
            isRV64Enabled: Boolean
        ) {
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
                System.out.printf("Testing: %s%n", instruction.mnemonic)
                when (instruction.instructionFormat) {
                    BasicInstructionFormat.B_FORMAT, BasicInstructionFormat.J_FORMAT -> {
                        continue
                    }

                    else -> {}
                }
                val format = instruction.exampleFormat

                program.assembleString(format)
                program.setup(mutableListOf<String>(), "")
                val instructionAddress = MemoryConfiguration.DEFAULT.textBaseAddress
                val word = program.memory.getWord(instructionAddress)

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
                program.setup(mutableListOf<String>(), "")
                val word2 = program.memory.getWord(instructionAddress)
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
                program.setup(mutableListOf<String>(), "")
                val first = program.memory.getWord(0x400000)
                val second = program.memory.getWord(0x400004)
                val ps = ProgramStatement(first, 0x400000)
                assertNotNull(ps.instruction, "Error 11 on: $programString")
                assertThat<String>(
                    "Error 12 on: $programString",
                    ps.printableBasicAssemblyStatement,
                    CoreMatchers.not<String?>(CoreMatchers.containsString("invalid"))
                )
                if (programString.contains("t0") || programString.contains("t1") || programString.contains("t2") || programString.contains(
                        "f1"
                    )
                ) {
                    // TODO: test that each register individually is meaningful and test every
                    // register.
                    // Currently this covers all instructions and is an alert if I made a trivial
                    // mistake.
                    val registerSubstitute =
                        programString.replace("t0|t1|t2".toRegex(), "x0").replace("f1".toRegex(), "f0")
                    program.assembleString(registerSubstitute)
                    program.setup(mutableListOf<String>(), "")
                    val word1 = program.memory.getWord(0x400000)
                    val word2 = program.memory.getWord(0x400004)
                    Assertions.assertFalse(word1 == first && word2 == second, "Error 13 on: $programString")
                }
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun runSingle() {
        val path = "riscv-tests/fadd.s"
        runTest(testDataPath.resolve(path).toString(), false)
    }

    @Test
    @Throws(AddressErrorException::class)
    fun testBasicInstructionBinaryCodes32() {
        testBasicInstructionBinaryCodesImpl(false)
    }

    @Test
    @Throws(AddressErrorException::class)
    fun testBasicInstructionBinaryCodes64() {
        testBasicInstructionBinaryCodesImpl(true)
    }

    @Test
    @Throws(Exception::class)
    fun testPseudoInstructions32() {
        testPseudoInstructionsImpl(false)
    }

    @Test
    @Throws(Exception::class)
    fun testPseudoInstructions64() {
        testPseudoInstructionsImpl(true)
    }

    @DisplayName("32 bit instructions")
    @ParameterizedTest
    @MethodSource("rv32TestFileProvider")
    @Throws(IOException::class)
    fun test32(path: Path) {
        runTest(path.toString(), false)
    }

    @DisplayName("64 bit instructions")
    @ParameterizedTest
    @MethodSource("rv64TestFileProvider")
    @Throws(IOException::class)
    fun test64(path: Path) {
        runTest(path.toString(), true)
    }

    @DisplayName("Examples")
    @ParameterizedTest
    @MethodSource("examplesTestFileProvider")
    @Throws(IOException::class)
    fun testExamples(path: Path) {
        runTest(path.toString(), false)
    }
}
