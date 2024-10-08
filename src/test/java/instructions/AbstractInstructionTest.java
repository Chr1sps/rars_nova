package instructions;

import org.jetbrains.annotations.NotNull;
import rars.ErrorMessage;
import rars.Globals;
import rars.Settings;
import rars.api.Options;
import rars.api.Program;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.InstructionSet;
import rars.simulator.Simulator;
import utils.RarsTestBase;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractInstructionTest extends RarsTestBase {

    protected final void runTest32(@NotNull final String code) {
        runTest(code, false, new TestData());
    }

    protected final void runTest32(@NotNull final String code, @NotNull final TestData testData) {
        runTest(code, false, testData);
    }

    protected final void runTest64(@NotNull final String code) {
        runTest(code, true, new TestData());
    }

    protected final void runTest64(@NotNull final String code, @NotNull final TestData testData) {
        runTest(code, true, testData);
    }

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
        final var errors = doRun(finalCode, program, testData);
        assertEquals("", errors, errors);
    }

    private @NotNull String doRun(@NotNull final String code, @NotNull final Program program, @NotNull final TestData testData) {
        try {
            program.assembleString(code);
            if (!testData.errorLines.isEmpty()) {
                return "Expected assembly error, but successfully assembled `" + getTestName() + "`.";
            }
            program.setup(null, testData.stdin);
            final Simulator.Reason r = program.simulate();
            if (r != Simulator.Reason.NORMAL_TERMINATION) {
                return "Ended abnormally while executing `" + getTestName() + "`.";
            } else {
                if (program.getExitCode() != 42) {
                    return "Final exit code was wrong for `" + getTestName() + "`.";
                }
                if (!program.getSTDOUT().equals(testData.stdout)) {
                    return "STDOUT was wrong for `" + getTestName() + "`.\n Expected:\n\"" + testData.stdout + "\",\nbut got \"" + program.getSTDOUT() + "\".";
                }
                if (!program.getSTDERR().equals(testData.stderr)) {
                    return "STDERR was wrong for `" + getTestName() + "`.\n Expected:\n\"" + testData.stdout + "\",\nbut got \"" + program.getSTDERR() + "\".";
                }
                return "";
            }
        } catch (final AssemblyException ae) {
            if (testData.errorLines.isEmpty()) {
                return "Failed to assemble `" + getTestName() + "`.";
            }
            if (ae.errors().errorCount() != testData.errorLines.size()) {
                return "Mismatched number of assembly errors in `" + getTestName() + "`.";
            }
            final Iterator<ErrorMessage> errors = ae.errors().getErrorMessages().iterator();
            for (final int number : testData.errorLines) {
                ErrorMessage error = errors.next();
                while (error.isWarning()) error = errors.next();
                if (error.getLine() != number) {
                    return "Expected error on line " + number + ". Found error on line " + error.getLine() + " in `" + getTestName() + "`.";
                }
            }
            return "";
        } catch (final SimulationException se) {
            return "Crashed while executing `" + getTestName() + "`.";
        }
    }

    protected static final class TestData {
        public @NotNull String stdin;
        public @NotNull String stdout;
        public @NotNull String stderr;
        public @NotNull List<Integer> errorLines;

        public TestData() {
            this.stdin = "";
            this.stdout = "";
            this.stderr = "";
            this.errorLines = List.of();
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
