import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rars.ProgramStatement;
import rars.api.Program;
import rars.api.ProgramOptions;
import rars.exceptions.AddressErrorException;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.MemoryConfiguration;
import rars.settings.BoolSetting;
import rars.simulator.Simulator;
import utils.RarsTestBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static rars.settings.BoolSettings.BOOL_SETTINGS;

final class AppTest extends RarsTestBase {
    // TODO: refactor this class to avoid repetitions and to enhance test speed

    private static void run(final String path, final boolean is64Bit) throws IOException {
        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, is64Bit);
        InstructionsRegistry.RV64_MODE_FLAG = is64Bit;

        final var errorLines = new HashSet<Integer>();
        String stdin = "", stdout = "", stderr = "";
        // TODO: better config system
        // This is just a temporary solution that should work for the tests I want to
        // write
        try (final BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            while (line.startsWith("#")) {
                if (line.startsWith("#error on lines:")) {
                    final String[] linenumbers = line.replaceFirst("#error on lines:", "").split(",");
                    for (final var num : linenumbers) {
                        errorLines.add(Integer.parseInt(num.trim()));
                    }
                } else if (line.startsWith("#stdin:")) {
                    stdin = line.replaceFirst("#stdin:", "").replaceAll("\\\\n", "\n");
                } else if (line.startsWith("#stdout:")) {
                    stdout = line.replaceFirst("#stdout:", "").replaceAll("\\\\n", "\n");
                } else if (line.startsWith("#stderr:")) {
                    stderr = line.replaceFirst("#stderr:", "").replaceAll("\\\\n", "\n");
                }
                line = br.readLine();
            }
        }
        final var programArgs = new ProgramOptions();
        programArgs.startAtMain = true;
        programArgs.maxSteps = 1000;
        programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT;
        final var program = new Program(programArgs);
        try {
            program.assembleFile(new File(path));
            if (!errorLines.isEmpty()) {
                fail("Expected assembly error, but successfully assembled `" + path + "`.\n");
            }
            program.setup(List.of(), stdin);
            System.out.println("Machine list:");
            program.getMachineList().forEach(System.out::println);
            System.out.println();
            final Simulator.Reason r = program.simulate();
            if (r != Simulator.Reason.NORMAL_TERMINATION) {
                final var msg = "Ended abnormally while executing `" + path + "`.\n" +
                    "Reason: " + r + ".\n";
                fail(msg);
            } else {
                if (program.getExitCode() != 42) {
                    final var msg = "Final exit code was wrong for `" + path + "`.\n" +
                        "Expected: 42, but got " + program.getExitCode() + ".";
                    fail(msg);
                }
                if (!program.getSTDOUT().equals(stdout)) {
                    final var msg = "STDOUT was wrong for `" + path + "`.\n" +
                        "Expected:\n\"" + stdout + "\",\nbut got \"" + program.getSTDOUT() + "\".";
                    fail(msg);
                }
                if (!program.getSTDERR().equals(stderr)) {
                    final var msg = "STDERR was wrong for `" + path + "`.\n" +
                        "Expected:\n\"" + stderr + "\",\nbut got \"" + program.getSTDERR() + "\".";
                    fail(msg);
                }
            }
        } catch (final AssemblyException ae) {
            if (errorLines.isEmpty()) {
                final var builder = new StringBuilder();
                builder.append("Failed to assemble `%s` due to following error(s):\n".formatted(path));
                for (final var error : ae.errors.getErrorMessages()) {
                    builder.append("[%d, %d] %s\n".formatted(
                        error.getLineNumber(),
                        error.getPosition(),
                        error.getMessage()
                    ));
                }
                fail(builder.toString());
            }
            final var errors = ae.errors.getErrorMessages();
            final var foundErrorLines = new HashSet<Integer>();
            for (final var error : errors) {
                if (error.isWarning()) continue;
                foundErrorLines.add(error.getLineNumber());
            }
            if (!errorLines.equals(foundErrorLines)) {
                final var builder = new StringBuilder();
                builder.append("Expected and actual error lines are not equal for `%s`.\n".formatted(path));
                builder.append("Expected lines: %s\n".formatted(errorLines));
                builder.append("Errors found:\n");
                for (final var error : errors) {
                    builder.append("[%d,%d] %s\n".formatted(
                        error.getLineNumber(),
                        error.getPosition(),
                        error.getMessage()
                    ));
                }
                fail(builder.toString());
            }

        } catch (final SimulationException se) {
            final var msg = """
                Crashed while executing `%s`.
                Reason: %s.
                Value: %d.
                Message: %s.""".formatted(
                path,
                se.reason,
                se.value,
                se.errorMessage.getMessage()
            );
            fail(msg);
        }
    }

    private static Stream<Named<Path>> fileProvider(final @NotNull String directory) throws IOException {
        final var path = getTestDataPath().resolve(directory);
        // noinspection resource
        return Files.walk(path).filter(p -> Files.isRegularFile(p) && p.getFileName()
                .toString()
                .toLowerCase()
                .endsWith(".s"))
            .map(p -> Named.of(p.getFileName().toString(), p));
    }

    static Stream<Named<Path>> rv32TestFileProvider() throws IOException {
        return fileProvider("riscv-tests");
    }

    static Stream<Named<Path>> rv64TestFileProvider() throws IOException {
        return fileProvider("riscv-tests-64");
    }

    static Stream<Named<Path>> examplesTestFileProvider() throws IOException {
        return fileProvider("examples");
    }

    private static void testBasicInstructionBinaryCodesImpl(
        final boolean isRV64Enabled
    ) throws AssemblyException, AddressErrorException {
        final var programArgs = new ProgramOptions();
        programArgs.startAtMain = true;
        programArgs.maxSteps = 500;
        programArgs.selfModifyingCode = true;
        programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT;
        final var program = new Program(programArgs);

        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true);
        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, isRV64Enabled);
        InstructionsRegistry.RV64_MODE_FLAG = isRV64Enabled;

        final var instructionsToTest = isRV64Enabled
            ? InstructionsRegistry.BASIC_INSTRUCTIONS.r64All
            : InstructionsRegistry.BASIC_INSTRUCTIONS.r32All;
        for (final var instruction : instructionsToTest) {
            System.out.printf("Testing: %s%n", instruction.mnemonic);
            if (instruction.getInstructionFormat() == BasicInstructionFormat.B_FORMAT || instruction.getInstructionFormat() == BasicInstructionFormat.J_FORMAT) {
                continue;
            }

            final String format = instruction.exampleFormat;

            program.assembleString(format);
            program.setup(List.of(), "");
            final var instructionAddress = MemoryConfiguration.DEFAULT.textBaseAddress;
            final int word = program.getMemory().getWord(instructionAddress);

            final var baseStatement = program.getMachineList().getFirst();
            final var statementFromMemory = new ProgramStatement(word, instructionAddress);

            final var message = """
                Expected:  %s
                Actual:    %s
                """.formatted(baseStatement, statementFromMemory);
            System.out.println(message);
            assertNotNull(statementFromMemory.getInstruction(), "Error 1 on: " + format);
            assertThat(
                "Error 2 on: " + format,
                statementFromMemory.getPrintableBasicAssemblyStatement(),
                not(containsString(
                    "invalid"))
            );

            program.assembleString(format);
            program.setup(List.of(), "");
            final int word2 = program.getMemory().getWord(instructionAddress);
            assertEquals(word, word2, "Error 3 on: " + format);
            assertEquals(instruction, statementFromMemory.getInstruction(), "Error 4 on: " + format);
        }
    }

    private static void testPseudoInstructionsImpl(final boolean isRV64) {
        final var programArgs = new ProgramOptions();
        programArgs.startAtMain = true;
        programArgs.maxSteps = 500;
        programArgs.selfModifyingCode = true;
        programArgs.memoryConfiguration = MemoryConfiguration.DEFAULT;
        final var program = new Program(programArgs);
        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true);
        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, isRV64);
        InstructionsRegistry.RV64_MODE_FLAG = isRV64;

        final var instructionsToTest = isRV64
            ? InstructionsRegistry.EXTENDED_INSTRUCTIONS.r64All
            : InstructionsRegistry.EXTENDED_INSTRUCTIONS.r32All;
        for (final var instruction : instructionsToTest) {
            final var programString = "label:" + instruction.exampleFormat;
            try {
                program.assembleString(programString);
                program.setup(List.of(), "");
                final int first = program.getMemory().getWord(0x400000);
                final int second = program.getMemory().getWord(0x400004);
                final ProgramStatement ps = new ProgramStatement(first, 0x400000);
                assertNotNull(ps.getInstruction(), "Error 11 on: " + programString);
                assertThat(
                    "Error 12 on: " + programString, ps.getPrintableBasicAssemblyStatement(),
                    not(containsString("invalid"))
                );
                if (programString.contains("t0")
                    || programString.contains("t1")
                    || programString.contains("t2")
                    || programString.contains("f1")) {
                    // TODO: test that each register individually is meaningful and test every
                    // register.
                    // Currently this covers all instructions and is an alert if I made a trivial
                    // mistake.
                    final var register_substitute = programString
                        .replaceAll("t0|t1|t2", "x0")
                        .replaceAll("f1", "f0");
                    program.assembleString(register_substitute);
                    program.setup(List.of(), "");
                    final int word1 = program.getMemory().getWord(0x400000);
                    final int word2 = program.getMemory().getWord(0x400004);
                    assertFalse(word1 == first && word2 == second, "Error 13 on: " + programString);
                }
            } catch (final Exception e) {
                throw new RuntimeException("Error 14 on" + programString + " :" + e);
            }
        }
    }

    @DisplayName("32 bit instructions")
    @ParameterizedTest
    @MethodSource("rv32TestFileProvider")
    void test32(final @NotNull Path path) throws IOException {
        run(path.toString(), false);
    }

    @DisplayName("64 bit instructions")
    @ParameterizedTest
    @MethodSource("rv64TestFileProvider")
    void test64(final @NotNull Path path) throws IOException {
        run(path.toString(), true);
    }

    @DisplayName("Examples")
    @ParameterizedTest
    @MethodSource("examplesTestFileProvider")
    void testExamples(final @NotNull Path path) throws IOException {
        run(path.toString(), false);
    }

    @Test
    void runSingle() throws IOException {
        run(getTestDataPath().resolve("examples/success.s").toString(), false);
    }

    @Test
    void testBasicInstructionBinaryCodes32() throws AssemblyException, AddressErrorException {
        testBasicInstructionBinaryCodesImpl(false);
    }

    @Test
    void testBasicInstructionBinaryCodes64() throws AssemblyException, AddressErrorException {
        testBasicInstructionBinaryCodesImpl(true);
    }

    @Test
    void testPseudoInstructions32() {
        testPseudoInstructionsImpl(false);
    }

    @Test
    void testPseudoInstructions64() {
        testPseudoInstructionsImpl(true);
    }
}
