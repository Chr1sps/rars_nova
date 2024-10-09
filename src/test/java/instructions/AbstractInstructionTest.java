package instructions;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.Settings;
import rars.api.Options;
import rars.api.Program;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.InstructionSet;
import rars.simulator.Simulator;
import utils.RarsTestBase;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractInstructionTest extends RarsTestBase {

    protected final void runTest32(@NotNull final String code) {
        runTest(code, false, new TestData());
    }

    protected final void runTest32(@NotNull final String code, @NotNull final TestData testData) {
        runTest(code, false, testData);
    }

    /**
     * Runs a test with the given code for RV64 with no standard input/output and no errors.
     *
     * @param code A {@link String} containing the code to run.
     */
    protected final void runTest64(@NotNull final String code) {
        runTest(code, true, new TestData());
    }

    protected final void runTest64(@NotNull final String code, @NotNull final TestData testData) {
        runTest(code, true, testData);
    }

    /**
     * Runs a test with the given code and test data.
     *
     * @param code     A {@link String} containing the code to run.
     * @param is64     A boolean indicating whether the test is for RV64.
     * @param testData A {@link TestData} object containing the test data (STD{IN,OUT,ERR}, error lines).
     */
    protected final void runTest(@NotNull final String code, final boolean is64, final TestData testData) {
        Globals.initialize();
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, is64);
        InstructionSet.rv64 = is64;
        Globals.instructionSet.populate();

        final var opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 1000;
        final var program = new Program(opt);

        final var header = """
                .text
                main:
                
                """;

        final var passAndFail = """
                pass:
                    li a0, 42
                    li a7, 93
                    ecall
                fail:
                    li a0, 0
                    li a7, 93
                    ecall
                """;

        final var finalCode = header + code + passAndFail;
        doRun(finalCode, program, testData);
    }

    private void doRun(@NotNull final String code, @NotNull final Program program, @NotNull final TestData testData) {
        try {
            program.assembleString(code);
            if (!testData.errorLines.isEmpty()) {
                fail("Expected assembly error, but successfully assembled `" + getTestName() + "`.");
            }
            program.setup(null, testData.stdin);
            final Simulator.Reason r = program.simulate();
            if (r != Simulator.Reason.NORMAL_TERMINATION) {
                final var msg = "Ended abnormally while executing `" + getTestName() + "`.\n" +
                        "Reason: " + r + ".\n";
                fail(msg);
            } else {
                if (program.getExitCode() != 42) {
                    final var msg = "Final exit code was wrong for `" + getTestName() + "`.\n" +
                            "Expected: 42, but got " + program.getExitCode() + ".";
                    fail(msg);
                }
                if (!program.getSTDOUT().equals(testData.stdout)) {
                    final var msg = "STDOUT was wrong for `" + getTestName() + "`.\n" +
                            "Expected:\n\"" + testData.stdout + "\",\nbut got \"" + program.getSTDOUT() + "\".";
                    fail(msg);
                }
                if (!program.getSTDERR().equals(testData.stderr)) {
                    final var msg = "STDERR was wrong for `" + getTestName() + "`.\n" +
                            "Expected:\n\"" + testData.stderr + "\",\nbut got \"" + program.getSTDERR() + "\".";
                    fail(msg);
                }
            }
        } catch (final AssemblyException ae) {
            if (testData.errorLines.isEmpty()) {
                var builder = new StringBuilder();
                builder.append("Failed to assemble `" + getTestName() + "` due to following error(s):\n");
                for (final var error : ae.errors().getErrorMessages()) {
                    builder.append("[" + error.getLine() + "," + error.getPosition() + "] " + error.getMessage() + "\n");
                }
                fail(builder.toString());
            }
            final var errors = ae.errors().getErrorMessages();
            final var foundErrorLines = new HashSet<Integer>();
            for (final var error : errors) {
                if (error.isWarning()) continue;
                foundErrorLines.add(error.getLine());
            }
            if (!testData.errorLines.equals(foundErrorLines)) {
                final var builder = new StringBuilder();
                builder.append("Expected and actual error lines are not equal for `" + getTestName() + "`.\n");
                builder.append("Expected lines: " + testData.errorLines + "\n");
                builder.append("Errors found:\n");
                for (final var error : errors) {
                    builder.append("[" + error.getLine() + "," + error.getPosition() + "] " + error.getMessage() + "\n");
                }
                fail(builder.toString());
            }

        } catch (final SimulationException se) {
            final var msg = "Crashed while executing `" + getTestName() + "`.\n" +
                    "Reason: " + se.reason + ".\n" +
                    "Value: " + se.value + ".\n" +
                    "Message: " + se.errorMessage.getMessage() + ".";
            fail(msg);
        }
    }

    protected final void runArithmeticTest32(
            @NotNull final String op,
            @NotNull final String firstValue,
            @NotNull final String secondValue,
            @NotNull final String result) {
        final var finalCode = "li x1, " + firstValue + "\n" +
                "li x2, " + secondValue + "\n" +
                op + " x30, x1, x2\n" +
                "li x29, " + result + "\n" +
                "bne x30, x29, fail\n";
        runTest32(finalCode);
    }

    protected final void runArithmeticImmediateTest32(
            @NotNull final String op,
            @NotNull final String firstValue,
            @NotNull final String immediate,
            @NotNull final String result) {
        final var finalCode = "li x1, " + firstValue + "\n" +
                op + " x30, x1, " + immediate + "\n" +
                "li x29, " + result + "\n" +
                "bne x30, x29, fail\n";
        runTest32(finalCode);
    }

    protected static final class TestData {
        public @NotNull String stdin;
        public @NotNull String stdout;
        public @NotNull String stderr;
        public @NotNull Set<Integer> errorLines;

        public TestData() {
            this.stdin = "";
            this.stdout = "";
            this.stderr = "";
            this.errorLines = Set.of();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final var that = (TestData) obj;
            return Objects.equals(this.stdin, that.stdin) &&
                    Objects.equals(this.stdout, that.stdout) &&
                    Objects.equals(this.stderr, that.stderr) &&
                    Objects.equals(this.errorLines, that.errorLines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stdin, stdout, stderr, errorLines);
        }

        @Override
        public String toString() {
            return "TestData[" +
                    "stdin=" + stdin + ", " +
                    "stdout=" + stdout + ", " +
                    "stderr=" + stderr + ", " +
                    "errorLines=" + errorLines + ']';
        }
    }
}
