import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rars.ProgramStatement;
import rars.api.Options;
import rars.api.Program;
import rars.exceptions.AddressErrorException;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.BasicInstructionFormat;
import rars.riscv.Instruction;
import rars.riscv.InstructionsRegistry;
import rars.riscv.hardware.MemoryConfiguration;
import rars.settings.BoolSetting;
import rars.simulator.Simulator;
import utils.RarsTestBase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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

public final class AppTest extends RarsTestBase {
    private static void run(final String path, final boolean is64Bit) {
        BOOL_SETTINGS.setSetting(BoolSetting.RV64_ENABLED, is64Bit);
        InstructionsRegistry.RV64_MODE_FLAG = is64Bit;

        final var opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 1000;
        final var program = new Program(opt);

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
        } catch (final FileNotFoundException fe) {
            fail("Could not find file: " + path + ".\n");
        } catch (final IOException io) {
            fail("Error reading `" + path + "`.\n");
        }
        try {
            program.assembleFile(path);
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
                    builder.append("[%d, %d] %s\n".formatted(error.getLine(), error.getPosition(), error.getMessage()));
                }
                fail(builder.toString());
            }
            final var errors = ae.errors.getErrorMessages();
            final var foundErrorLines = new HashSet<Integer>();
            for (final var error : errors) {
                if (error.isWarning()) continue;
                foundErrorLines.add(error.getLine());
            }
            if (!errorLines.equals(foundErrorLines)) {
                final var builder = new StringBuilder();
                builder.append("Expected and actual error lines are not equal for `%s`.\n".formatted(path));
                builder.append("Expected lines: %s\n".formatted(errorLines));
                builder.append("Errors found:\n");
                for (final var error : errors) {
                    builder.append("[%d,%d] %s\n".formatted(error.getLine(), error.getPosition(), error.getMessage()));
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
        //noinspection resource
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

    static Stream<Named<Instruction>> instructionTestProvider() {
        return InstructionsRegistry.ALL_INSTRUCTIONS.r32All
            .stream()
            .map(instruction -> Named.of(instruction.mnemonic, instruction));
    }

    @DisplayName("32 bit instructions")
    @ParameterizedTest
    @MethodSource("rv32TestFileProvider")
    public void test32(final @NotNull Path path) {
        run(path.toString(), false);
    }

    @DisplayName("64 bit instructions")
    @ParameterizedTest
    @MethodSource("rv64TestFileProvider")
    void test64(final @NotNull Path path) {
        run(path.toString(), true);
    }

    @DisplayName("Examples")
    @ParameterizedTest
    @MethodSource("examplesTestFileProvider")
    void testExamples(final @NotNull Path path) {
        run(path.toString(), false);
    }

    @Tag("manual")
    @Test
    void runSingle() {
        run(getTestDataPath().resolve("riscv-tests-64/lui.S").toString(), true);
    }

    @Test
    void testInstructions() {
        final Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        final Program program = new Program(opt);
        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true);

        for (final var binst : InstructionsRegistry.BASIC_INSTRUCTIONS.allInstructions) {
            if (binst.getInstructionFormat() == BasicInstructionFormat.B_FORMAT || binst.getInstructionFormat() == BasicInstructionFormat.J_FORMAT) {
                continue;
            }

            final String format = binst.exampleFormat;
            try {
                program.assembleString(format);
                program.setup(List.of(), "");
                final var instructionAddress = MemoryConfiguration.DEFAULT.textBaseAddress;
                final int word = program.getMemory().getWord(instructionAddress);
                final var programStatement = new ProgramStatement(word, instructionAddress);
                assertNotNull(programStatement.getInstruction(), "Error 1 on: " + format);
                assertThat(
                    "Error 2 on: " + format,
                    programStatement.getPrintableBasicAssemblyStatement(),
                    not(containsString(
                        "invalid"))
                );

                program.assembleString(format);
                program.setup(List.of(), "");
                final int word2 = program.getMemory().getWord(instructionAddress);
                assertEquals(word, word2, "Error 3 on: " + format);
                assertEquals(binst, programStatement.getInstruction(), "Error 4 on: " + format);
                /*
                 * if (assembled.getInstruction() == null) {
                 * System.out.println("Error 5 on: " + program);
                 * continue;
                 * }
                 * if (assembled.getOperands().length != ps.getOperands().length){
                 * System.out.println("Error 6 on: " + program);
                 * continue;
                 * }
                 * for (int i = 0; i < assembled.getOperands().length; i++){
                 * if(assembled.getOperand(i) != ps.getOperand(i)){
                 * System.out.println("Error 7 on: " + program);
                 * }
                 * }
                 */

                /*
                 * // Not currently used
                 * // Do a bit of trial and error to test out variations
                 * decompiled =
                 * decompiled.replaceAll("x6","t1").replaceAll("x7","t2").replaceAll("x28","t3")
                 * .trim();
                 * String spaced_out = decompiled.replaceAll(",",", ");
                 * if(!program.equals(decompiled) && !program.equals(spaced_out)){
                 * Globals.getSettings().setBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX,
                 * false);
                 * decompiled = ps.getPrintableBasicAssemblyStatement();
                 * String decompiled2 =
                 * decompiled.replaceAll("x6","t1").replaceAll("x7","t2").replaceAll("x28","t3")
                 * .trim();
                 * String spaced_out2 = decompiled2.replaceAll(",",", ");
                 * if(!program.equals(decompiled2) && !program.equals(spaced_out2)) {
                 * System.out.println("Error 5 on: " + program;
                 * }
                 *
                 * Globals.getSettings().setBooleanSetting(Settings.Bool.DISPLAY_VALUES_IN_HEX,
                 * true);
                 * }
                 */
            } catch (final AssemblyException | AddressErrorException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Test
    void testPseudoInstructions() {
        final Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        final var p = new Program(opt);
        BOOL_SETTINGS.setSetting(BoolSetting.SELF_MODIFYING_CODE_ENABLED, true);

        int skips = 0;
        for (final var inst : InstructionsRegistry.EXTENDED_INSTRUCTIONS.allInstructions) {
            final String program = "label:" + inst.exampleFormat;
            try {
                p.assembleString(program);
                p.setup(List.of(), "");
                final int first = p.getMemory().getWord(0x400000);
                final int second = p.getMemory().getWord(0x400004);
                final ProgramStatement ps = new ProgramStatement(first, 0x400000);
                assertNotNull(ps.getInstruction(), "Error 11 on: " + program);
                assertThat(
                    "Error 12 on: " + program, ps.getPrintableBasicAssemblyStatement(),
                    not(containsString("invalid"))
                );
                if (program.contains("t0") || program.contains("t1") || program.contains("t2") || program.contains(
                    "f1")) {
                    // TODO: test that each register individually is meaningful and test every
                    // register.
                    // Currently this covers all instructions and is an alert if I made a trivial
                    // mistake.
                    final String register_substitute =
                        program.replaceAll("t0", "x0")
                            .replaceAll("t1", "x0")
                            .replaceAll("t2", "x0")
                            .replaceAll("f1", "f0");
                    p.assembleString(register_substitute);
                    p.setup(List.of(), "");
                    final int word1 = p.getMemory().getWord(0x400000);
                    final int word2 = p.getMemory().getWord(0x400004);
                    assertFalse(word1 == first && word2 == second, "Error 13 on: " + program);
                } else {
                    skips++;
                }
            } catch (final Exception e) {
                throw new RuntimeException("Error 14 on" + program + " :" + e);
            }
        }
        // 12 was the second when this test was written, if instructions are added that
        // intentionally
        // don't have those registers in them add to the register list above or add to
        // the count.
        // Updated to 10: because fsrmi and fsflagsi were removed
        assertEquals(10, skips, "Unexpected number of psuedo-instructions skipped.");
    }
}
