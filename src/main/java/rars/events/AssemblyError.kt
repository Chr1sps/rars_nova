package rars.events

import rars.ErrorList

/**
 * Class used mainly in Tokenizer and Assembler; represents errors that occur
 * while assembling a RISC-V program.
 *
 * @author Benjamin Landers
 * @version July 2017
 */
@JvmInline
value class AssemblyError(@JvmField val errors: ErrorList)
