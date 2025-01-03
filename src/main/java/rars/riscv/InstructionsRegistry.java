package rars.riscv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.assembler.TokenList;
import rars.assembler.Tokenizer;
import rars.exceptions.AssemblyException;
import rars.riscv.instructions.*;
import rars.riscv.instructions.compressed.CADDI4SPN;
import rars.riscv.instructions.compressed.CEBREAK;
import rars.riscv.instructions.compressed.CompressedJump;
import rars.settings.BoolSetting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static rars.settings.BoolSettings.BOOL_SETTINGS;
import static rars.util.Utils.concatStreams;

/**
 * This class contains all the defined RISC-V instructions. It is intended
 * to replace the previous InstructionSet class with a simpler implementation,
 * which doesn't rely on reflection. It takes into account the fact that the
 * instructions in the simulator *don't* change at runtime, so there is no need
 * to use the {@code populate()} method.
 */
public final class InstructionsRegistry {

    public static final @NotNull SingleInstructionSet<@NotNull BasicInstruction> BASIC_INSTRUCTIONS;
    public static final @NotNull SingleInstructionSet<@NotNull CompressedInstruction> COMPRESSED_INSTRUCTIONS;
    public static final @NotNull SingleInstructionSet<@NotNull ExtendedInstruction> EXTENDED_INSTRUCTIONS;
    public static final @NotNull SingleInstructionSet<@NotNull Instruction> ALL_INSTRUCTIONS;
    private static final @NotNull String PSEUDO_OPS_PATH = "/pseudoOps/";
    private static final @NotNull Logger LOGGER = LogManager.getLogger();
    private final static @NotNull List<@NotNull MatchMap> R32_MATCH_MAPS;
    private final static @NotNull List<@NotNull MatchMap> R64_MATCH_MAPS;
    private static final boolean initialized;
    private final static @NotNull Map<@NotNull Instruction, @NotNull TokenList> tokenListMap;
    public static boolean RV64_MODE_FLAG;

    static {
        BASIC_INSTRUCTIONS = new SingleInstructionSet<>(
                List.of(
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
                        URET.INSTANCE,
                        WFI.INSTANCE
                        // endregion Other

                ),
                List.of(
                        SLLI32.INSTANCE,
                        SRAI32.INSTANCE,
                        SRLI32.INSTANCE
                ),
                List.of(
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
                        SLLI64.INSTANCE,
                        SLLIW.INSTANCE,
                        SRAI64.INSTANCE,
                        SRAIW.INSTANCE,
                        SRLI64.INSTANCE,
                        SRLIW.INSTANCE
                        // endregion Other
                )
        );
        COMPRESSED_INSTRUCTIONS = new SingleInstructionSet<>(
                List.of(
                        CADDI4SPN.INSTANCE,
                        CEBREAK.INSTANCE,
                        CompressedJump.CJ
                ),
                List.of(),
                List.of()
        );
        EXTENDED_INSTRUCTIONS = new SingleInstructionSet<>(
                loadPseudoInstructions("shared.txt"),
                loadPseudoInstructions("rv32_only.txt"),
                loadPseudoInstructions("rv64_only.txt")
        );
        ALL_INSTRUCTIONS = SingleInstructionSet.concat(BASIC_INSTRUCTIONS, EXTENDED_INSTRUCTIONS/*, COMPRESSED_INSTRUCTIONS*/);
    }

    static {
        RV64_MODE_FLAG = BOOL_SETTINGS.getSetting(BoolSetting.RV64_ENABLED);
        R32_MATCH_MAPS = createMatchMaps(BASIC_INSTRUCTIONS.r32All);
        R64_MATCH_MAPS = createMatchMaps(BASIC_INSTRUCTIONS.r64All);
        tokenListMap = createTokenListMap();
        initialized = true;
    }

    private InstructionsRegistry() {
    }

    public static @Nullable BasicInstruction findBasicInstructionByBinaryCode(final int binaryCode) {
        final var matchMaps = RV64_MODE_FLAG ? R64_MATCH_MAPS : R32_MATCH_MAPS;
        return matchMaps.stream()
                .filter(matchMap -> matchMap.find(binaryCode) != null)
                .findAny()
                .map(matchMap -> matchMap.find(binaryCode))
                .orElse(null);
    }

    public static @NotNull List<@NotNull Instruction> matchOperator(final @NotNull String operator) {
        final var instructionSet = initialized ? ALL_INSTRUCTIONS : BASIC_INSTRUCTIONS;
        final var instructionsToSearch = RV64_MODE_FLAG ? instructionSet.r64All : instructionSet.r32All;
        return instructionsToSearch.stream()
                .filter(instruction -> instruction.mnemonic.equalsIgnoreCase(operator))
                .map(instruction -> (Instruction) instruction)
                .toList();
    }

    public static @NotNull List<@NotNull Instruction> matchOperatorByPrefix(final @NotNull String operator) {
        final var instructionsToSearch = RV64_MODE_FLAG ? BASIC_INSTRUCTIONS.r64All : BASIC_INSTRUCTIONS.r32All;
        return instructionsToSearch.stream()
                .filter(instruction -> instruction.mnemonic.toLowerCase().startsWith(operator.toLowerCase()))
                .map(instruction -> (Instruction) instruction)
                .toList();
    }

    private static @NotNull List<@NotNull ExtendedInstruction> loadPseudoInstructions(final @NotNull String filename) {
        final var instructionList = new ArrayList<@NotNull ExtendedInstruction>();
        try (final var stream = InstructionsRegistry.class.getResourceAsStream(PSEUDO_OPS_PATH + filename)) {
            if (stream == null) {
                LOGGER.error("Error: Could not load pseudo instructions from file: {}", filename);
                System.exit(1);
            }
            final var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));
            String line;
            while ((line = reader.readLine()) != null) {
                // skip over: comment lines, empty lines, lines starting with blank.
                if (!line.startsWith("#") && !line.startsWith(" ")
                        && !line.isEmpty()) {
                    final var tokenizer = new StringTokenizer(line, ";");
                    final String pseudoOp = tokenizer.nextToken();
                    final var template = new StringBuilder();
                    while (tokenizer.hasMoreTokens()) {
                        final String token = tokenizer.nextToken();
                        if (token.startsWith("#")) {
                            // Optional description must be last token in the line.
                            final var description = token.substring(1);
                            instructionList.add(new ExtendedInstruction(pseudoOp, template.toString(), description));
                            break;
                        }
                        template.append(token);
                        if (tokenizer.hasMoreTokens()) {
                            template.append("\n");
                        }
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return instructionList;
    }

    private static @NotNull List<@NotNull MatchMap> createMatchMaps(
            final @NotNull List<? extends BasicInstruction> instructionList) {
        final var maskMap = new HashMap<Integer, HashMap<Integer, BasicInstruction>>();
        final var matchMaps = new ArrayList<MatchMap>();
        for (final var instruction : instructionList) {
            final var mask = instruction.opcodeMask;
            final var match = instruction.opcodeMatch;
            var matchMap = maskMap.get(mask);
            if (maskMap.get(mask) == null) {
                matchMap = new HashMap<>();
                maskMap.put(mask, matchMap);
                matchMaps.add(new MatchMap(mask, matchMap));
            }
            matchMap.put(match, instruction);
        }
        return matchMaps.stream().sorted().toList();
    }

    private static @NotNull Map<Instruction, TokenList> createTokenListMap() {
        final var result = new HashMap<Instruction, TokenList>();
        for (final var instruction : ALL_INSTRUCTIONS.allInstructions) {
            final var exampleFormat = instruction.exampleFormat;
            try {
                final var tokenList = Tokenizer.tokenizeExampleInstruction(exampleFormat);
                result.put(instruction, tokenList);
            } catch (final AssemblyException e) {
                InstructionsRegistry.LOGGER.error(
                        "CONFIGURATION ERROR: Instruction example \"{}\" contains invalid token(s)" +
                                ".", exampleFormat
                );
            }
        }
        return result;
    }

    public static @NotNull TokenList getTokenList(final @NotNull Instruction instruction) {
        return tokenListMap.get(instruction);
    }

    private static class MatchMap implements Comparable<@NotNull MatchMap> {
        private final int mask;
        private final int maskLength; // number of '1' bits in mask
        private final @NotNull Map<@NotNull Integer, @NotNull BasicInstruction> matchMap;

        public MatchMap(final int mask, final @NotNull Map<@NotNull Integer, @NotNull BasicInstruction> matchMap) {
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
            if (d == 0) {
                d = this.mask - other.mask;
            }
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

    /**
     * A simple utility class for grouping instructions based on their 32/64-bit
     * mode availability.
     */
    public static final class SingleInstructionSet<T extends @NotNull Instruction> {
        /**
         * Instructions only present in 32-bit mode.
         */
        public final @NotNull List<? extends @NotNull T> r32Only;
        /**
         * Instructions only present in 64-bit mode.
         */
        public final @NotNull List<? extends @NotNull T> r64Only;
        /**
         * Instructions present in both 32-bit and 64-bit modes.
         */
        public final @NotNull List<? extends @NotNull T> shared;
        /**
         * 32-bit only + shared instructions.
         */
        public final @NotNull List<? extends @NotNull T> r32All;
        /**
         * 64-bit only + shared instructions.
         */
        public final @NotNull List<? extends @NotNull T> r64All;
        /**
         * All instructions (32-bit only + 64-bit only + shared).
         */
        public final @NotNull List<? extends @NotNull T> allInstructions;

        public SingleInstructionSet(
                final @NotNull List<? extends @NotNull T> shared,
                final @NotNull List<? extends @NotNull T> r32Only,
                final @NotNull List<? extends @NotNull T> r64Only
        ) {
            this.r32Only = r32Only;
            this.r64Only = r64Only;
            this.shared = shared;
            this.r32All = concatStreams(shared.stream(), r32Only.stream()).toList();
            this.r64All = concatStreams(shared.stream(), r64Only.stream()).toList();
            this.allInstructions = concatStreams(
                    shared.stream(),
                    r32Only.stream(),
                    r64Only.stream()
            ).collect(Collectors.toList());
        }

        @SafeVarargs
        public static <T extends Instruction> @NotNull SingleInstructionSet<@NotNull T> concat(
                final @NotNull SingleInstructionSet<? extends @NotNull T>... others) {
            final var r32Only = concatStreams(
                    Arrays.stream(others).map(s -> s.r32Only).flatMap(List::stream)
            ).toList();
            final var r64Only = concatStreams(
                    Arrays.stream(others).map(s -> s.r64Only).flatMap(List::stream)
            ).toList();
            final var shared = concatStreams(
                    Arrays.stream(others).map(s -> s.shared).flatMap(List::stream)
            ).toList();
            return new SingleInstructionSet<>(shared, r32Only, r64Only);
        }
    }
}
