package compressedInstructions;

import org.jetbrains.annotations.NotNull;
import rars.ErrorList;
import rars.api.Program;
import rars.api.ProgramOptions;
import rars.exceptions.AssemblyException;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.MemoryConfiguration;
import rars.settings.BoolSetting;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static rars.settings.BoolSettings.BOOL_SETTINGS;

public abstract class AbstractCompressedInstructionTest {

    protected void doTest(
        final @NotNull String code,
        final boolean isRV64,
        final boolean hasErrors
    ) {

        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, isRV64);
        InstructionsRegistry.RV64_MODE_FLAG = isRV64;

        System.out.println("═══════SOURCE═CODE═══════");
        System.out.println("```");
        System.out.println(code);
        System.out.println("```");
        System.out.println();

        final var programOptions = new ProgramOptions();
        programOptions.memoryConfiguration = MemoryConfiguration.DEFAULT;
        final var program = new Program(programOptions);
        ErrorList errorList;
        try {
            errorList = program.assembleString(code);

            System.out.println("═══════PARSED══CODE══════");
            program.getParsedList().forEach((s) -> System.out.println(s.toString()));
            System.out.println();

            System.out.println("═══════MACHINE═CODE══════");
            program.getMachineList().forEach((s) -> System.out.println(s.toString()));
            System.out.println();

        } catch (final AssemblyException e) {
            errorList = e.errors;
        }

        final var report = errorList.generateErrorAndWarningReport();
        System.out.println("═══════DIAGNOSTICS═══════");
        System.out.println(report);
        System.out.println();
        System.out.println("═════════════════════════");

        if (hasErrors && !errorList.errorsOccurred()) {
            fail("Expected errors, but got none.");
        } else if (!hasErrors && errorList.errorsOccurred()) {
            fail("Unexpected errors occurred.");
        }
    }

    protected void assertCompiles(final @NotNull String code) {
        doTest(code, false, false);
    }

    protected void assertCompiles64(final @NotNull String code) {
        doTest(code, true, false);
    }

    protected void assertFails(final @NotNull String code) {
        doTest(code, false, true);
    }

    protected void assertFails64(final @NotNull String code) {
        doTest(code, true, true);
    }

    private void assertErrors(
        final @NotNull List<ErrorEntry> expectedErrors,
        final @NotNull ErrorList errorList
    ) {
        final var errors = errorList.getErrorMessages();
        if (errors.size() != expectedErrors.size()) {
            fail("Expected " + expectedErrors.size() + " errors, but got " + errors.size());
        }

        for (int i = 0; i < errors.size(); i++) {
            final var expected = expectedErrors.get(i);
            final var actual = errors.get(i);
            if (expected.line() != actual.getLineNumber() || expected.position() != actual.getPosition()) {
                fail("Expected error at line " + expected.line() + " position " + expected.position() +
                    ", but got error at line " + actual.getLineNumber() + " position " + actual.getPosition());
            }
        }
    }

    protected record ErrorEntry(int line, int position, boolean isWarning) {
        public static @NotNull ErrorEntry error(final int line, final int position) {
            return new ErrorEntry(line, position, false);
        }

        public static @NotNull ErrorEntry warning(final int line, final int position) {
            return new ErrorEntry(line, position, true);
        }
    }

}
