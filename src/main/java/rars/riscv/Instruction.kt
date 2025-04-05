package rars.riscv

import java.util.*

/**
 * Base class to represent member of RISCV instruction set.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
sealed class Instruction protected constructor(
    /**
     * Example usage of this instruction. Is provided as subclass constructor
     * argument.
     */
    @JvmField val exampleFormat: String,
    /**
     * Description of instruction for display to user
     */
    @JvmField val description: String
) {
    /** The instruction name. */
    @JvmField
    val mnemonic: String = extractOperator(exampleFormat)

    abstract val instructionLength: Int

    companion object {
        /**
         * Characters used in instruction mask to indicate bit positions
         * for 'f'irst, 's'econd, 't'hird, 'q'uad, and 'p'enta operands .
         */
        @JvmField
        val operandMask: CharArray = charArrayOf('f', 's', 't', 'q', 'p')

        /**
         * Used by subclass constructors to extract operator mnemonic from the
         * instruction example.
         */
        protected fun extractOperator(example: String): String {
            val st = StringTokenizer(example, " ,\t")
            return st.nextToken()
        }
    }
}
