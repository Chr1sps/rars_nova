package instructions

import TestIO
import doRunString
import rars.Globals
import rars.api.Program
import rars.api.ProgramOptions
import rars.riscv.InstructionsRegistry
import rars.riscv.hardware.memory.MemoryConfiguration
import rars.settings.BoolSetting
import utils.RarsTestBase

@Suppress("ReplacePrintlnWithLogging")
abstract class AbstractInstructionTest : RarsTestBase() {
    protected fun runTest32(code: String) {
        runTest(code, "", false, TestIO())
    }

    protected fun runTest32(code: String, testData: TestIO) {
        runTest(code, "", false, testData)
    }

    /**
     * Runs a test with the given code for RV64 with no standard input/output and no errors.
     *
     * @param code
     * A [String] containing the code to run.
     */
    protected fun runTest64(code: String) {
        runTest(code, "", true, TestIO())
    }

    protected fun runTest64(code: String, dataSegment: String) {
        runTest(code, dataSegment, true, TestIO())
    }

    protected fun runTest64(code: String, testData: TestIO) {
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
     * A [TestIO] object containing the test data (STD{IN,OUT,ERR}, error lines).
     */
    private fun runTest(
        code: String, dataPrelude: String, is64: Boolean,
        testData: TestIO
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
        doRunString(finalCode, program, testData)
    }

    protected fun runArithmeticTest32(
        op: String,
        firstValue: String,
        secondValue: String,
        result: String
    ) {
        val finalCode = """
            li x1, $firstValue
            li x2, $secondValue
            $op x30, x1, x2
            li x29, $result
            bne x30, x29, fail
        """.trimIndent()
        runTest32(finalCode)
    }

    protected fun runArithmeticImmediateTest32(
        op: String,
        firstValue: String,
        immediate: String,
        result: String
    ) {
        val finalCode = """
            li x1, $firstValue
            $op x30, x1, $immediate
            li x29, $result
            bne x30, x29, fail
        """.trimIndent()
        runTest32(finalCode)
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
