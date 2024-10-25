package rars.riscv;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.Globals;
import rars.Settings;
import rars.riscv.instructions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class contains all the defined RISC-V instructions. It is intended
 * to replace the previous InstructionSet class with a simpler implementation,
 * which doesn't rely on reflection. It takes into account the fact that the
 * instructions in the simulator *don't* change at runtime, so there is no need
 * to use the <code>populate()</code> method.
 */
public final class Instructions {
    public final static List<BasicInstruction> INSTRUCTIONS_R32;
    public final static List<BasicInstruction> INSTRUCTIONS_R64;

    public final static List<ExtendedInstruction> INSTRUCTIONS_R32_EXTENDED;
    public final static List<ExtendedInstruction> INSTRUCTIONS_R64_EXTENDED;
    public final static List<Instruction> INSTRUCTIONS_ALL;
    private final static List<Instruction> INSTRUCTIONS_ALL_R32_ONLY;
    private final static List<Instruction> INSTRUCTIONS_ALL_R64_ONLY;
    private final static List<MatchMap> R32_MATCH_MAPS;
    private final static List<MatchMap> R64_MATCH_MAPS;
    private static final boolean initialized;
    public static boolean RV64;

    static {
        INSTRUCTIONS_R32 = List.of(
                // region Arithmetic
                ADD.INSTANCE,
                AND.INSTANCE,
                DIV.INSTANCE,
                DIVU.INSTANCE,
                MUL.INSTANCE,
                MULH.INSTANCE,
                MULHSU.INSTANCE,
                MULHU.INSTANCE,
                OR.INSTANCE,
                REM.INSTANCE,
                REMU.INSTANCE,
                SLL.INSTANCE,
                SLT.INSTANCE,
                SLTU.INSTANCE,
                SRA.INSTANCE,
                SRL.INSTANCE,
                SUB.INSTANCE,
                XOR.INSTANCE,
                // endregion branch

                // region Branch
                BEQ.INSTANCE,
                BGE.INSTANCE,
                BGEU.INSTANCE,
                BLT.INSTANCE,
                BLTU.INSTANCE,
                BNE.INSTANCE,
                // endregion

                // region Double
                FADDD.INSTANCE,
                FDIVD.INSTANCE,
                FMAXD.INSTANCE,
                FMIND.INSTANCE,
                FMULD.INSTANCE,
                FSUBD.INSTANCE,
                // endregion Double

                // region Floating
                FADDS.INSTANCE,
                FDIVS.INSTANCE,
                FMAXS.INSTANCE,
                FMINS.INSTANCE,
                FMULS.INSTANCE,
                FSUBS.INSTANCE,
                // endregion Floating

                // region FusedDouble
                FMADDD.INSTANCE,
                FMSUBD.INSTANCE,
                FNMADDD.INSTANCE,
                FNMSUBD.INSTANCE,
                // endregion FusedDouble

                // region FusedFloat
                FMADDS.INSTANCE,
                FMSUBS.INSTANCE,
                FNMADDS.INSTANCE,
                FNMSUBS.INSTANCE,
                // endregion FusedFloat

                // region ImmediateInstruction
                ADDI.INSTANCE,
                ANDI.INSTANCE,
                ORI.INSTANCE,
                SLTI.INSTANCE,
                SLTIU.INSTANCE,
                XORI.INSTANCE,
                // endregion ImmediateInstruction

                // region Load
                LB.INSTANCE,
                LBU.INSTANCE,
                LH.INSTANCE,
                LHU.INSTANCE,
                LW.INSTANCE,
                // endregion Load

                // region Store
                SB.INSTANCE,
                SH.INSTANCE,
                SW.INSTANCE,
                // endregion Store

                // region Other
                AUIPC.INSTANCE,
                CSRRC.INSTANCE,
                CSRRCI.INSTANCE,
                CSRRS.INSTANCE,
                CSRRSI.INSTANCE,
                CSRRW.INSTANCE,
                CSRRWI.INSTANCE,
                EBREAK.INSTANCE,
                ECALL.INSTANCE,
                FCLASSD.INSTANCE,
                FCLASSS.INSTANCE,
                FCVTDS.INSTANCE,
                FCVTDW.INSTANCE,
                FCVTDWU.INSTANCE,
                FCVTSD.INSTANCE,
                FCVTSW.INSTANCE,
                FCVTSWU.INSTANCE,
                FCVTWD.INSTANCE,
                FCVTWS.INSTANCE,
                FCVTWUD.INSTANCE,
                FCVTWUS.INSTANCE,
                FENCE.INSTANCE,
                FENCEI.INSTANCE,
                FEQD.INSTANCE,
                FEQS.INSTANCE,
                FLD.INSTANCE,
                FLED.INSTANCE,
                FLES.INSTANCE,
                FLTD.INSTANCE,
                FLTS.INSTANCE,
                FLW.INSTANCE,
                FMVSX.INSTANCE,
                FMVXS.INSTANCE,
                FSD.INSTANCE,
                FSGNJD.INSTANCE,
                FSGNJND.INSTANCE,
                FSGNJNS.INSTANCE,
                FSGNJS.INSTANCE,
                FSGNJXD.INSTANCE,
                FSGNJXS.INSTANCE,
                FSQRTD.INSTANCE,
                FSQRTS.INSTANCE,
                FSW.INSTANCE,
                JAL.INSTANCE,
                JALR.INSTANCE,
                LUI.INSTANCE,
                SLLI.INSTANCE,
                SRAI.INSTANCE,
                SRLI.INSTANCE,
                URET.INSTANCE,
                WFI.INSTANCE
                // endregion Other

        );
        INSTRUCTIONS_R64 = List.of(
                // region ArithmeticW
                ADDW.INSTANCE,
                DIVUW.INSTANCE,
                DIVW.INSTANCE,
                MULW.INSTANCE,
                REMUW.INSTANCE,
                REMW.INSTANCE,
                SLLW.INSTANCE,
                SRAW.INSTANCE,
                SRLW.INSTANCE,
                SUBW.INSTANCE,
                // endregion ArithmeticW

                // ImmediateInstruction
                ADDIW.INSTANCE,

                // Load
                LD.INSTANCE,
                LWU.INSTANCE,

                // Store
                SD.INSTANCE,

                // region Other
                FCVTDL.INSTANCE,
                FCVTDLU.INSTANCE,
                FCVTLD.INSTANCE,
                FCVTLS.INSTANCE,
                FCVTLUD.INSTANCE,
                FCVTLUS.INSTANCE,
                FCVTSL.INSTANCE,
                FCVTSLU.INSTANCE,
                FMVDX.INSTANCE,
                FMVXD.INSTANCE,
                SLLIW.INSTANCE,
                SRAIW.INSTANCE,
                SRLIW.INSTANCE
                // endregion Other
        );
        INSTRUCTIONS_R32_EXTENDED = loadPseudoInstructions("/PseudoOps.txt");
        INSTRUCTIONS_R64_EXTENDED = loadPseudoInstructions("/PseudoOps-64.txt");

        INSTRUCTIONS_ALL_R32_ONLY = Stream
                .concat(INSTRUCTIONS_R32.stream(), INSTRUCTIONS_R32_EXTENDED.stream())
                .collect(Collectors.toList());

        INSTRUCTIONS_ALL_R64_ONLY = Stream
                .concat(INSTRUCTIONS_R64.stream(), INSTRUCTIONS_R64_EXTENDED.stream())
                .collect(Collectors.toList());

        INSTRUCTIONS_ALL = Stream.of(
                        INSTRUCTIONS_R32,
                        INSTRUCTIONS_R64,
                        INSTRUCTIONS_R32_EXTENDED,
                        INSTRUCTIONS_R64_EXTENDED
                ).flatMap(Collection::stream)
                .collect(Collectors.toList());

        RV64 = Globals.getSettings().getBooleanSetting(Settings.Bool.RV64_ENABLED);

        R32_MATCH_MAPS = createMatchMaps(INSTRUCTIONS_R32);
        R64_MATCH_MAPS = createMatchMaps(INSTRUCTIONS_R64);
        initialized = true;
    }

    private Instructions() {
    }

    public static @Nullable BasicInstruction findBasicInstructionByBinaryCode(final int binaryCode) {
        for (final var matchMap : R32_MATCH_MAPS) {
            final var instruction = matchMap.find(binaryCode);
            if (instruction != null) {
                return instruction;
            }
        }
        if (RV64) {
            for (final var matchMap : R64_MATCH_MAPS) {
                final var instruction = matchMap.find(binaryCode);
                if (instruction != null) {
                    return instruction;
                }
            }
        }
        return null;
    }

    public static @NotNull List<Instruction> matchOperator(final @NotNull String operator) {
        final var matchingInstructions = new ArrayList<Instruction>();
        if (initialized) {
            INSTRUCTIONS_ALL_R32_ONLY.stream()
                    .filter(instruction -> instruction.getName().equalsIgnoreCase(operator))
                    .forEach(matchingInstructions::add);
            if (RV64)
                INSTRUCTIONS_ALL_R64_ONLY.stream()
                        .filter(instruction -> instruction.getName().equalsIgnoreCase(operator))
                        .forEach(matchingInstructions::add);
        } else {
            INSTRUCTIONS_R32.stream()
                    .filter(instruction -> instruction.getName().equalsIgnoreCase(operator))
                    .forEach(matchingInstructions::add);
            if (RV64)
                INSTRUCTIONS_R64.stream()
                        .filter(instruction -> instruction.getName().equalsIgnoreCase(operator))
                        .forEach(matchingInstructions::add);
        }
        return matchingInstructions;
    }

    public static @NotNull List<Instruction> matchOperatorByPrefix(final @NotNull String operator) {
        final var matchingInstructions = new ArrayList<Instruction>();
        INSTRUCTIONS_ALL_R32_ONLY.stream()
                .filter(instruction -> instruction.getName().toLowerCase().startsWith(operator.toLowerCase()))
                .forEach(matchingInstructions::add);
        if (RV64)
            INSTRUCTIONS_ALL_R64_ONLY.stream()
                    .filter(instruction -> instruction.getName().toLowerCase().startsWith(operator.toLowerCase()))
                    .forEach(matchingInstructions::add);
        return matchingInstructions;
    }

    private static @NotNull List<ExtendedInstruction> loadPseudoInstructions(final @NotNull String filename) {
        final var instructionList = new ArrayList<ExtendedInstruction>();
        try (
                final InputStream stream = Instructions.class.getResourceAsStream(filename);
                final var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                // skip over: comment lines, empty lines, lines starting with blank.
                if (!line.startsWith("#") && !line.startsWith(" ")
                        && !line.isEmpty()) {
                    var description = "";
                    final var tokenizer = new StringTokenizer(line, ";");
                    final String pseudoOp = tokenizer.nextToken();
                    final var template = new StringBuilder();
                    while (tokenizer.hasMoreTokens()) {
                        final String token = tokenizer.nextToken();
                        if (token.startsWith("#")) {
                            // Optional description must be last token in the line.
                            description = token.substring(1);
                            break;
                        }
                        template.append(token);
                        if (tokenizer.hasMoreTokens()) {
                            template.append("\n");
                        }
                    }
                    instructionList.add(new ExtendedInstruction(pseudoOp, template.toString(), description));
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return instructionList;
    }

    private static @NotNull List<MatchMap> createMatchMaps(final @NotNull List<BasicInstruction> instructionList) {
        final var maskMap = new HashMap<Integer, HashMap<Integer, BasicInstruction>>();
        final var matchMaps = new ArrayList<MatchMap>();
        for (final var instruction : instructionList) {
            final var mask = instruction.getOpcodeMask();
            final var match = instruction.getOpcodeMatch();
            var matchMap = maskMap.get(mask);
            if (matchMap == null) {
                matchMap = new HashMap<>();
                maskMap.put(mask, matchMap);
                matchMaps.add(new MatchMap(mask, matchMap));
            }
            matchMap.put(match, instruction);
        }
        Collections.sort(matchMaps);
        return matchMaps;
    }

    private static class MatchMap implements Comparable<MatchMap> {
        private final int mask;
        private final int maskLength; // number of 1 bits in mask
        private final HashMap<Integer, BasicInstruction> matchMap;

        public MatchMap(final int mask, final HashMap<Integer, BasicInstruction> matchMap) {
            this.mask = mask;
            this.matchMap = matchMap;

            int k = 0;
            int n = mask;
            while (n != 0) {
                k++;
                n &= n - 1;
            }
            this.maskLength = k;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof MatchMap && this.mask == ((MatchMap) o).mask;
        }

        @Override
        public int compareTo(final @NotNull MatchMap other) {
            int d = other.maskLength - this.maskLength;
            if (d == 0)
                d = this.mask - other.mask;
            return d;
        }

        public @Nullable BasicInstruction find(final int instr) {
            final int match = instr & this.mask;
            return this.matchMap.get(match);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mask, maskLength, matchMap);
        }
    }
}
