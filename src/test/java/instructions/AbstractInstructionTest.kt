package instructions

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import rars.Globals
import rars.api.Program
import rars.api.ProgramOptions
import rars.exceptions.SimulationException
import rars.riscv.InstructionsRegistry
import rars.riscv.hardware.MemoryConfiguration
import rars.settings.BoolSetting
import rars.simulator.Simulator
import rars.util.Utils
import utils.RarsTestBase
import java.util.*

@Suppress("ReplacePrintlnWithLogging")
abstract class AbstractInstructionTest : RarsTestBase() {
    protected fun runTest32(code: String) {
        runTest(code, "", false, TestData())
    }

    protected fun runTest32(code: String, testData: TestData) {
        runTest(code, "", false, testData)
    }

    /**
     * Runs a test with the given code for RV64 with no standard input/output and no errors.
     *
     * @param code
     * A [String] containing the code to run.
     */
    protected fun runTest64(code: String) {
        runTest(code, "", true, TestData())
    }

    protected fun runTest64(code: String, data: String) {
        runTest(code, data, true, TestData())
    }

    protected fun runTest64(code: String, testData: TestData) {
        runTest(code, "", true, testData)
    }

    /**
     * Runs a test with the given code and test data.
     *
     * @param code
     * A [String] containing the code to run.
     * @param is64
     * A boolean indicating whether the test is for RV64.
     * @param testData
     * A [TestData] object containing the test data (STD{IN,OUT,ERR}, error lines).
     */
    private fun runTest(
        code: String, dataPrelude: String, is64: Boolean,
        testData: TestData
    ) {
        Globals.BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, is64)
        InstructionsRegistry.RV64_MODE_FLAG = is64

        val programArgs = ProgramOptions()
        programArgs.startAtMain = true
        programArgs.maxSteps = 1000
        programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT
        val program = Program(programArgs)

        val finalCode: String = getDecoratedCode(code, dataPrelude)
        println("═══════GENERATED═CODE═══════")
        println(finalCode)
        println("════════════════════════════")
        doRun(finalCode, program, testData)
    }

    private fun doRun(code: String, program: Program, testData: TestData) {
        try {
            program.assembleString(code).onLeft { assemblyError ->
                if (testData.errorLines.isEmpty()) {
                    fail {
                        buildString {
                            this.append("Failed to assemble `${this@AbstractInstructionTest.testName}` due to following error(s):\n")
                            for (error in assemblyError.errors.errorMessages) {
                                this.append("[${error.lineNumber},${error.position}] ${error.message}\n")
                            }
                        }
                    }
                }
                val errors = assemblyError.errors.errorMessages
                val foundErrorLines = HashSet<Int?>()
                for (error in errors) {
                    if (error.isWarning) continue
                    foundErrorLines.add(error.lineNumber)
                }
                if (testData.errorLines != foundErrorLines) {
                    fail {
                        buildString {
                            this.append(
                                """
                            Expected and actual error lines are not equal for `${this@AbstractInstructionTest.testName}`.
                            Expected lines: ${testData.errorLines}
                            Errors found:
                            """.trimIndent()
                            )
                            for (error in errors) {
                                this.append("[${error.lineNumber},${error.position}] ${error.message}\n")
                            }
                        }
                    }
                }
                return
            }
            if (!testData.errorLines.isEmpty()) {
                Assertions.fail<Any?>("Expected assembly error, but successfully assembled `$testName`.")
            }
            program.setup(mutableListOf<String>(), testData.stdin)
            val r = program.simulate()
            if (r != Simulator.Reason.NORMAL_TERMINATION) {
                val msg = "Ended abnormally while executing `" + testName + "`.\n" +
                        "Reason: " + r + ".\n"
                Assertions.fail<Any?>(msg)
            } else {
                if (program.exitCode != 42) {
                    val msg = "Final exit code was wrong for `" + testName + "`.\n" +
                            "Expected: 42, but got " + program.exitCode + "."
                    Assertions.fail<Any?>(msg)
                }
                if (program.sTDOUT != testData.stdout) {
                    val msg = "STDOUT was wrong for `" + testName + "`.\n" +
                            "Expected:\n\"" + testData.stdout + "\",\nbut got \"" + program.sTDOUT + "\"."
                    Assertions.fail<Any?>(msg)
                }
                if (program.sTDERR != testData.stderr) {
                    val msg = "STDERR was wrong for `" + testName + "`.\n" +
                            "Expected:\n\"" + testData.stderr + "\",\nbut got \"" + program.sTDERR + "\"."
                    Assertions.fail<Any?>(msg)
                }
            }
        } catch (se: SimulationException) {
            val msg = "Crashed while executing `" + testName + "`.\n" +
                    "Reason: " + se.reason + "\n" +
                    "Value: " + se.value + "\n" +
                    "Message: " + se.errorMessage!!.message + "\n" +
                    "Stacktrace: " + Utils.getStacktraceString(se) + "\n"

            Assertions.fail<Any?>(msg)
        }
    }

    protected fun runArithmeticTest32(
        op: String,
        firstValue: String,
        secondValue: String,
        result: String
    ) {
        val finalCode = "li x1, " + firstValue + "\n" +
                "li x2, " + secondValue + "\n" +
                op + " x30, x1, x2\n" +
                "li x29, " + result + "\n" +
                "bne x30, x29, fail\n"
        runTest32(finalCode)
    }

    protected fun runArithmeticImmediateTest32(
        op: String,
        firstValue: String,
        immediate: String,
        result: String
    ) {
        val finalCode = "li x1, " + firstValue + "\n" +
                op + " x30, x1, " + immediate + "\n" +
                "li x29, " + result + "\n" +
                "bne x30, x29, fail\n"
        runTest32(finalCode)
    }

    protected class TestData {
        var stdin: String = ""
        var stdout: String = ""
        var stderr: String = ""
        var errorLines: MutableSet<Int?> = mutableSetOf<Int?>()

        override fun equals(obj: Any?): Boolean {
            if (obj === this) return true
            if (obj == null || obj.javaClass != this.javaClass) return false
            val that: TestData = obj as TestData
            return this.stdin == that.stdin &&
                    this.stdout == that.stdout &&
                    this.stderr == that.stderr &&
                    this.errorLines == that.errorLines
        }

        override fun hashCode(): Int {
            return Objects.hash(stdin, stdout, stderr, errorLines)
        }

        override fun toString(): String {
            return "TestData[" +
                    "stdin=" + stdin + ", " +
                    "stdout=" + stdout + ", " +
                    "stderr=" + stderr + ", " +
                    "errorLines=" + errorLines + ']'
        }
    }

    companion object {
        private fun getDecoratedCode(code: String, dataPrelude: String): String {
            var header = """
            # PRELUDE
            .text
            main:
            
            # TEST CODE
            
            """.trimIndent()
            if (!dataPrelude.isEmpty()) {
                header = """
                # DATA
                .data
                $dataPrelude
                $header
                """.trimIndent()
            }

            val passAndFail = """
            
            # EPILOGUE
            pass:
                li a0, 42
                li a7, 93
                ecall
            fail:
                li a0, 0
                li a7, 93
                ecall
            
            """.trimIndent()

            return header + code + passAndFail
        }
    }
}
