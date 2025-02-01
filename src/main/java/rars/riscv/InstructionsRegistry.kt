package rars.riscv

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import rars.Globals
import rars.assembler.TokenList
import rars.assembler.Tokenizer.Companion.tokenizeExampleInstruction
import rars.riscv.instructions.*
import rars.settings.BoolSetting
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.system.exitProcess

/**
 * This class contains all the defined RISC-V instructions. It is intended
 * to replace the previous InstructionSet class with a simpler implementation,
 * which doesn't rely on reflection. It takes into account the fact that the
 * instructions in the simulator *don't* change at runtime, so there is no need
 * to use the `populate()` method.
 */
object InstructionsRegistry {
    private var initialized = false

    @JvmField
    val BASIC_INSTRUCTIONS: SingleInstructionSet<BasicInstruction> = SingleInstructionSet<BasicInstruction>(
        listOf(
            // region Arithmetic
            Arithmetic.ADD,
            Arithmetic.AND,
            Arithmetic.DIV,
            Arithmetic.DIVU,
            Arithmetic.MUL,
            Arithmetic.MULH,
            Arithmetic.MULHSU,
            Arithmetic.MULHU,
            Arithmetic.OR,
            Arithmetic.REM,
            Arithmetic.REMU,
            Arithmetic.SLL,
            Arithmetic.SLT,
            Arithmetic.SLTU,
            Arithmetic.SRA,
            Arithmetic.SRL,
            Arithmetic.SUB,
            Arithmetic.XOR,
            // endregion Arithmetic           

            // region Branch
            Branch.BEQ,
            Branch.BGE,
            Branch.BGEU,
            Branch.BLT,
            Branch.BLTU,
            Branch.BNE,
            // endregion Branch

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
            FSUBS.INSTANCE,  // endregion Floating
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
            BasicInstructions.AUIPC,
            BasicInstructions.CSRRC,
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
        listOf(
            SLLI32.INSTANCE,
            SRAI32.INSTANCE,
            SRLI32.INSTANCE
        ),
        listOf(
            // region ArithmeticW
            ArithmeticW.ADDW,
            ArithmeticW.DIVUW,
            ArithmeticW.DIVW,
            ArithmeticW.MULW,
            ArithmeticW.REMUW,
            ArithmeticW.REMW,
            ArithmeticW.SLLW,
            ArithmeticW.SRAW,
            ArithmeticW.SRLW,
            ArithmeticW.SUBW,
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
    )

    @JvmField
    val EXTENDED_INSTRUCTIONS: SingleInstructionSet<ExtendedInstruction> = SingleInstructionSet<ExtendedInstruction>(
        loadPseudoInstructions("shared.txt"),
        loadPseudoInstructions("rv32_only.txt"),
        loadPseudoInstructions("rv64_only.txt")
    )

    @JvmField
    val ALL_INSTRUCTIONS: SingleInstructionSet<Instruction> = SingleInstructionSet.Companion.concat<Instruction>(
        BASIC_INSTRUCTIONS,
        EXTENDED_INSTRUCTIONS /*, COMPRESSED_INSTRUCTIONS*/
    )

    private const val PSEUDO_OPS_PATH = "/pseudoOps/"
    private val LOGGER: Logger = LogManager.getLogger(InstructionsRegistry::class.java)

    @JvmField
    var RV64_MODE_FLAG: Boolean = Globals.BOOL_SETTINGS.getSetting(BoolSetting.RV64_ENABLED)

    private val R32_MATCH_MAPS = createMatchMaps(BASIC_INSTRUCTIONS.r32All)
    private val R64_MATCH_MAPS: List<MatchMap> = createMatchMaps(BASIC_INSTRUCTIONS.r64All)
    private val tokenListMap: Map<Instruction, TokenList> = createTokenListMap()

    init {
        initialized = true
    }

    @JvmStatic
    fun findBasicInstructionByBinaryCode(binaryCode: Int): BasicInstruction? {
        val matchMaps = if (RV64_MODE_FLAG) R64_MATCH_MAPS else R32_MATCH_MAPS
        return matchMaps
            .find { matchMap -> matchMap.find(binaryCode) != null }
            ?.find(binaryCode)
    }

    @JvmStatic
    fun matchOperator(operator: String): List<Instruction> {
        val instructionSet = if (initialized) ALL_INSTRUCTIONS else BASIC_INSTRUCTIONS
        val instructionsToSearch = if (RV64_MODE_FLAG) instructionSet.r64All else instructionSet.r32All
        return instructionsToSearch.filter { instruction -> instruction.mnemonic.equals(operator, ignoreCase = true) }
    }

    fun matchOperatorByPrefix(operator: String): List<Instruction> {
        val instructionsToSearch = if (RV64_MODE_FLAG) BASIC_INSTRUCTIONS.r64All else BASIC_INSTRUCTIONS.r32All
        return instructionsToSearch.filter { instruction ->
            instruction.mnemonic.startsWith(
                operator,
                ignoreCase = true
            )
        }
    }

    private fun loadPseudoInstructions(filename: String): MutableList<ExtendedInstruction> {
        val instructionList = ArrayList<ExtendedInstruction>()
        try {
            InstructionsRegistry::class.java.getResourceAsStream(PSEUDO_OPS_PATH + filename).use { stream ->
                if (stream == null) {
                    LOGGER.error("Error: Could not load pseudo instructions from file: {}", filename)
                    exitProcess(1)
                }
                val reader = BufferedReader(InputStreamReader(Objects.requireNonNull<InputStream?>(stream)))
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    // skip over: comment lines, empty lines, lines starting with blank.
                    if (!line!!.startsWith("#") && !line.startsWith(" ") && !line.isEmpty()) {
                        val tokenizer = StringTokenizer(line, ";")
                        val pseudoOp = tokenizer.nextToken()
                        val template = StringBuilder()
                        while (tokenizer.hasMoreTokens()) {
                            val token = tokenizer.nextToken()
                            if (token.startsWith("#")) {
                                // Optional description must be last token in the line.
                                val description = token.substring(1)
                                instructionList.add(ExtendedInstruction(pseudoOp, template.toString(), description))
                                break
                            }
                            template.append(token)
                            if (tokenizer.hasMoreTokens()) {
                                template.append("\n")
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return instructionList
    }

    private fun createMatchMaps(
        instructionList: List<BasicInstruction>
    ): List<MatchMap> {
        val maskMap = mutableMapOf<Int, MutableMap<Int, BasicInstruction>>()
        val matchMaps = mutableListOf<MatchMap>()
        for (instruction in instructionList) {
            val mask = instruction.opcodeMask
            val match = instruction.opcodeMatch
            var matchMap = maskMap[mask]
            if (matchMap == null) {
                matchMap = mutableMapOf<Int, BasicInstruction>()
                maskMap.put(mask, matchMap)
                matchMaps.add(MatchMap(mask, matchMap))
            }
            matchMap.put(match, instruction)
        }
        return matchMaps.sorted()
    }

    private fun createTokenListMap(): MutableMap<Instruction, TokenList> {
        val result = mutableMapOf<Instruction, TokenList>()
        for (instruction in ALL_INSTRUCTIONS.allInstructions) {
            val exampleFormat = instruction.exampleFormat
            tokenizeExampleInstruction(exampleFormat).fold(
                {
                    LOGGER.error(
                        "CONFIGURATION ERROR: Instruction example \"{}\" contains invalid token(s)",
                        exampleFormat
                    )
                },
                { result.put(instruction, it) }
            )
        }
        return result
    }

    @JvmStatic
    fun getTokenList(instruction: Instruction): TokenList {
        return tokenListMap[instruction]!!
    }

    private class MatchMap(private val mask: Int, private val matchMap: MutableMap<Int, BasicInstruction>) :
        Comparable<MatchMap> {
        private val maskLength: Int // number of '1' bits in mask

        init {
            var k = 0
            var n = mask
            while (n != 0) {
                k++
                n = n and n - 1
            }
            this.maskLength = k
        }

        override fun equals(o: Any?): Boolean {
            return o is MatchMap && this.mask == o.mask
        }

        override fun compareTo(other: MatchMap): Int {
            var d = other.maskLength - this.maskLength
            if (d == 0) {
                d = this.mask - other.mask
            }
            return d
        }

        fun find(instr: Int): BasicInstruction? {
            val match = instr and this.mask
            return this.matchMap[match]
        }

        override fun hashCode(): Int {
            return Objects.hash(mask, maskLength, matchMap)
        }
    }

    /**
     * A simple utility class for grouping instructions based on their 32/64-bit
     * mode availability.
     */
    class SingleInstructionSet<out T : Instruction>(
        /** Instructions present in both 32-bit and 64-bit modes. */
        @JvmField
        val shared: List<T>,
        /** Instructions only present in 32-bit mode. */
        @JvmField
        val r32Only: List<T>,
        /** Instructions only present in 64-bit mode. */
        @JvmField
        val r64Only: List<T>
    ) {
        /** 32-bit only + shared instructions. */
        @JvmField
        val r32All = shared + r32Only

        /** 64-bit only + shared instructions. */
        @JvmField
        val r64All = shared + r64Only

        /** All instructions (32-bit only + 64-bit only + shared). */
        @JvmField
        val allInstructions = shared + r32Only + r64Only

        companion object {
            @SafeVarargs
            fun <T : Instruction> concat(vararg sets: SingleInstructionSet<out T>): SingleInstructionSet<T> {
                val r32Only = sets.map { it.r32Only }.reduce(List<T>::plus)
                val r64Only = sets.map { it.r64Only }.reduce(List<T>::plus)
                val shared = sets.map { it.shared }.reduce(List<T>::plus)
                return SingleInstructionSet(shared, r32Only, r64Only)
            }
        }
    }
}
