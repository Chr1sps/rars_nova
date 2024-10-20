import org.junit.jupiter.api.Test;
import rars.Globals;
import rars.ProgramStatement;
import rars.Settings;
import rars.api.Options;
import rars.api.Program;
import rars.exceptions.AssemblyException;
import rars.exceptions.SimulationException;
import rars.riscv.*;
import rars.simulator.Simulator;
import utils.RarsTestBase;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest extends RarsTestBase {
    public static void runTestFiles(final String path, final boolean is64Bit) {
        Globals.initialize();
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, is64Bit);
        InstructionSet.rv64 = is64Bit;
        Globals.instructionSet.populate();

        final var opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 1000;
        final var p = new Program(opt);

        final var tests = new File(path).listFiles();

        for (final var test : Objects.requireNonNull(tests)) {
            if (test.isFile() && test.getName().toLowerCase().endsWith(".s")) {
                run(test.getPath(), p);
            }
        }
    }

    public static void run(final String path, final Program program) {
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
            program.assemble(path);
            if (!errorLines.isEmpty()) {
                fail("Expected assembly error, but successfully assembled `" + path + "`.\n");
            }
            program.setup(null, stdin);
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
                builder.append("Failed to assemble `" + path + "` due to following error(s):\n");
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
            if (!errorLines.equals(foundErrorLines)) {
                final var builder = new StringBuilder();
                builder.append("Expected and actual error lines are not equal for `" + path + "`.\n");
                builder.append("Expected lines: " + errorLines + "\n");
                builder.append("Errors found:\n");
                for (final var error : errors) {
                    builder.append("[" + error.getLine() + "," + error.getPosition() + "] " + error.getMessage() + "\n");
                }
                fail(builder.toString());
            }

        } catch (final SimulationException se) {
            final var msg = "Crashed while executing `" + path + "`.\n" +
                    "Reason: " + se.reason + ".\n" +
                    "Value: " + se.value + ".\n" +
                    "Message: " + se.errorMessage.getMessage() + ".";
            fail(msg);
        }
    }

    @Test
    public void test32() {
        runTestFiles(getTestDataPath().resolve("riscv-tests").toString(), false);
    }

    @Test
    public void test64() {
        runTestFiles(getTestDataPath().resolve("riscv-tests-64").toString(), true);
    }

    @Test
    public void testInstructions() {
        final Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        final Program p = new Program(opt);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, true);

        final ArrayList<Instruction> insts = Globals.instructionSet.getInstructionList();
        for (final Instruction inst : insts) {
            if (inst instanceof final BasicInstruction binst) {
                if (binst.getInstructionFormat() == BasicInstructionFormat.B_FORMAT || binst.getInstructionFormat() == BasicInstructionFormat.J_FORMAT)
                    continue;

                final String program = inst.getExampleFormat();
                try {
                    p.assembleString(program);
                    p.setup(null, "");
                    final int word = p.getMemory().getWord(0x400000);
//                    ProgramStatement assembled = p.getMemory().getStatement(0x400000);
                    final ProgramStatement ps = new ProgramStatement(word, 0x400000);
                    assertNotNull(ps.getInstruction(), "Error 1 on: " + program);
                    assertThat("Error 2 on: " + program, ps.getPrintableBasicAssemblyStatement(), not(containsString("invalid")));
//                    String decompiled = ps.getPrintableBasicAssemblyStatement();

                    p.assembleString(program);
                    p.setup(null, "");
                    final int word2 = p.getMemory().getWord(0x400000);
                    assertEquals(word, word2, "Error 3 on: " + program);
                    assertEquals(binst, ps.getInstruction(), "Error 4 on: " + program);
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
                } catch (final Exception e) {
                    throw new RuntimeException("Error 5 on " + program + ": " + e);
                }
            }
        }
    }

    @Test
    public void testPseudoInstructions() {
        final Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        final var p = new Program(opt);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, true);

        final var insts = Globals.instructionSet.getInstructionList();
        int skips = 0;
        for (final Instruction inst : insts) {
            if (inst instanceof ExtendedInstruction) {
                final String program = "label:" + inst.getExampleFormat();
                try {
                    p.assembleString(program);
                    p.setup(null, "");
                    final int first = p.getMemory().getWord(0x400000);
                    final int second = p.getMemory().getWord(0x400004);
                    final ProgramStatement ps = new ProgramStatement(first, 0x400000);
                    assertNotNull(ps.getInstruction(), "Error 11 on: " + program);
                    assertThat("Error 12 on: " + program, ps.getPrintableBasicAssemblyStatement(), not(containsString("invalid")));
                    if (program.contains("t0") || program.contains("t1") || program.contains("t2") || program.contains("f1")) {
                        // TODO: test that each register individually is meaningful and test every
                        // register.
                        // Currently this covers all instructions and is an alert if I made a trivial
                        // mistake.
                        final String register_substitute = program.replaceAll("t0", "x0").replaceAll("t1", "x0").replaceAll("t2", "x0").replaceAll("f1", "f0");
                        p.assembleString(register_substitute);
                        p.setup(null, "");
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
        }
        // 12 was the second when this test was written, if instructions are added that
        // intentionally
        // don't have those registers in them add to the register list above or add to
        // the count.
        // Updated to 10: because fsrmi and fsflagsi were removed
        assertEquals(10, skips, "Unexpected number of psuedo-instructions skipped.");
    }
}
