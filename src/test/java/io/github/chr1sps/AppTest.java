package io.github.chr1sps;

import io.github.chr1sps.rars.ErrorMessage;
import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.Settings;
import io.github.chr1sps.rars.api.Options;
import io.github.chr1sps.rars.api.Program;
import io.github.chr1sps.rars.exceptions.AssemblyException;
import io.github.chr1sps.rars.exceptions.SimulationException;
import io.github.chr1sps.rars.riscv.*;
import io.github.chr1sps.rars.simulator.Simulator;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

public class AppTest {
    static final String test_path = "src/test/resources";
/*

    @Test
    public void testGeneral() {
        runTestFiles(test_path, false);
    }
*/

    @Test
    public void test32() {
        runTestFiles(test_path + "/riscv-tests", false);
    }

    @Test
    public void test64() {
        runTestFiles(test_path + "/riscv-tests-64", true);
    }

    public static void runTestFiles(String path, boolean is64Bit) {
        Globals.initialize();
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.RV64_ENABLED, is64Bit);
        InstructionSet.rv64 = is64Bit;
        Globals.instructionSet.populate();

        var opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 1000;
        var p = new Program(opt);

        var tests = new File(path).listFiles();

        for (var test : Objects.requireNonNull(tests)) {
            if (test.isFile() && test.getName().toLowerCase().endsWith(".s")) {
                var errors = run(test.getPath(), p);
                Assert.assertEquals(errors, "", errors);
            }
        }
    }

    public static String run(String path, Program p) {
        int[] errorlines = null;
        String stdin = "", stdout = "", stderr = "";
        // TODO: better config system
        // This is just a temporary solution that should work for the tests I want to
        // write
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            while (line.startsWith("#")) {
                if (line.startsWith("#error on lines:")) {
                    String[] linenumbers = line.replaceFirst("#error on lines:", "").split(",");
                    errorlines = new int[linenumbers.length];
                    for (int i = 0; i < linenumbers.length; i++) {
                        errorlines[i] = Integer.parseInt(linenumbers[i].trim());
                    }
                } else if (line.startsWith("stdin:")) {
                    stdin = line.replaceFirst("#stdin:", "").replaceAll("\\\\n", "\n");
                } else if (line.startsWith("#stdout:")) {
                    stdout = line.replaceFirst("#stdout:", "").replaceAll("\\\\n", "\n");
                } else if (line.startsWith("#stderr:")) {
                    stderr = line.replaceFirst("#stderr:", "").replaceAll("\\\\n", "\n");
                }
                line = br.readLine();
            }
        } catch (FileNotFoundException fe) {
            return "Could not find " + path;
        } catch (IOException io) {
            return "Error reading " + path;
        }
        try {
            p.assemble(path);
            if (errorlines != null) {
                return "Expected asssembly error, but successfully assembled " + path;
            }
            p.setup(null, stdin);
            Simulator.Reason r = p.simulate();
            if (r != Simulator.Reason.NORMAL_TERMINATION) {
                return "Ended abnormally while executing " + path;
            } else {
                if (p.getExitCode() != 42) {
                    return "Final exit code was wrong for " + path;
                }
                if (!p.getSTDOUT().equals(stdout)) {
                    return "STDOUT was wrong for " + path + "\n Expected \"" + stdout + "\" got \"" + p.getSTDOUT() + "\"";
                }
                if (!p.getSTDERR().equals(stderr)) {
                    return "STDERR was wrong for " + path;
                }
                return "";
            }
        } catch (AssemblyException ae) {
            if (errorlines == null) {
                return "Failed to assemble " + path;
            }
            if (ae.errors().errorCount() != errorlines.length) {
                return "Mismatched number of assembly errors in" + path;
            }
            Iterator<ErrorMessage> errors = ae.errors().getErrorMessages().iterator();
            for (int number : errorlines) {
                ErrorMessage error = errors.next();
                while (error.isWarning()) error = errors.next();
                if (error.getLine() != number) {
                    return "Expected error on line " + number + ". Found error on line " + error.getLine() + " in " + path;
                }
            }
            return "";
        } catch (SimulationException se) {
            return "Crashed while executing " + path;
        }
    }

    @Test
    public void testInstructions() {
        Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        Program p = new Program(opt);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, true);

        ArrayList<Instruction> insts = Globals.instructionSet.getInstructionList();
        for (Instruction inst : insts) {
            if (inst instanceof BasicInstruction binst) {
                if (binst.getInstructionFormat() == BasicInstructionFormat.B_FORMAT || binst.getInstructionFormat() == BasicInstructionFormat.J_FORMAT)
                    continue;

                String program = inst.getExampleFormat();
                try {
                    p.assembleString(program);
                    p.setup(null, "");
                    int word = p.getMemory().getWord(0x400000);
//                    ProgramStatement assembled = p.getMemory().getStatement(0x400000);
                    ProgramStatement ps = new ProgramStatement(word, 0x400000);
                    Assert.assertNotNull("Error 1 on: " + program, ps.getInstruction());
                    MatcherAssert.assertThat("Error 2 on: " + program, ps.getPrintableBasicAssemblyStatement(), not(containsString("invalid")));
//                    String decompiled = ps.getPrintableBasicAssemblyStatement();

                    p.assembleString(program);
                    p.setup(null, "");
                    int word2 = p.getMemory().getWord(0x400000);
                    Assert.assertEquals("Error 3 on: " + program, word, word2);
                    Assert.assertEquals("Error 4 on: " + program, binst, ps.getInstruction());
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
                } catch (Exception e) {
                    throw new RuntimeException("Error 5 on " + program + ": " + e);
                }
            }
        }
    }

    @Test
    public void testPseudoInstructions() {
        Options opt = new Options();
        opt.startAtMain = true;
        opt.maxSteps = 500;
        opt.selfModifyingCode = true;
        var p = new Program(opt);
        Globals.getSettings().setBooleanSettingNonPersistent(Settings.Bool.SELF_MODIFYING_CODE_ENABLED, true);

        var insts = Globals.instructionSet.getInstructionList();
        int skips = 0;
        for (Instruction inst : insts) {
            if (inst instanceof ExtendedInstruction) {
                String program = "label:" + inst.getExampleFormat();
                try {
                    p.assembleString(program);
                    p.setup(null, "");
                    int first = p.getMemory().getWord(0x400000);
                    int second = p.getMemory().getWord(0x400004);
                    ProgramStatement ps = new ProgramStatement(first, 0x400000);
                    Assert.assertNotNull("Error 11 on: " + program, ps.getInstruction());
                    MatcherAssert.assertThat("Error 12 on: " + program, ps.getPrintableBasicAssemblyStatement(), not(containsString("invalid")));
                    if (program.contains("t0") || program.contains("t1") || program.contains("t2") || program.contains("f1")) {
                        // TODO: test that each register individually is meaningful and test every
                        // register.
                        // Currently this covers all instructions and is an alert if I made a trivial
                        // mistake.
                        String register_substitute = program.replaceAll("t0", "x0").replaceAll("t1", "x0").replaceAll("t2", "x0").replaceAll("f1", "f0");
                        p.assembleString(register_substitute);
                        p.setup(null, "");
                        int word1 = p.getMemory().getWord(0x400000);
                        int word2 = p.getMemory().getWord(0x400004);
                        Assert.assertFalse("Error 13 on: " + program, word1 == first && word2 == second);
                    } else {
                        skips++;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error 14 on" + program + " :" + e);
                }
            }
        }
        // 12 was the value when this test was written, if instructions are added that
        // intentionally
        // don't have those registers in them add to the register list above or add to
        // the count.
        // Updated to 10: because fsrmi and fsflagsi were removed
        Assert.assertEquals("Unexpected number of psuedo-instructions skipped.", 10, skips);
    }
}
