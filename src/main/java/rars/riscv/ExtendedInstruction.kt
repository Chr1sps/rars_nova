package rars.riscv

import rars.RISCVProgram
import rars.assembler.TokenList
import rars.util.BinaryUtilsOld
import rars.util.translateToInt
import rars.util.translateToLong
import java.util.*

/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/
/**
 * ExtendedInstruction represents a RISCV extended (a.k.a usePseudoInstructions) instruction.
 * This
 * assembly language instruction does not have a corresponding machine
 * instruction. Instead
 * it is translated by the extended assembler into one or more basic
 * instructions (operations
 * that have a corresponding machine instruction).
 *
 * @author Pete Sanderson
 * @version August 2003
 */
class ExtendedInstruction @JvmOverloads constructor(
    example: String,
    translation: String,
    description: String = ""
) : Instruction(example, description) {
    /**
     * List of Strings that represent list of templates for
     * basic instructions generated by this extended instruction.
     */
    val basicIntructionTemplateList = buildTranslationList(translation)


    /**
     * Length in bytes that this extended instruction requires in its
     * binary form. The answer depends on how many basic instructions it
     * expands to.
     */
    override val instructionLength = 4 * basicIntructionTemplateList.size

    companion object {
        /**
         * Given a basic instruction template and the list of tokens from an extended
         * instruction statement, substitute operands from the token list appropriately into the
         * template to generate the basic statement. Assumes the extended instruction statement has
         * been translated from source form to basic assembly form (e.g. register mnemonics
         * translated to corresponding register numbers).
         * Operand format of source statement is already verified correct.
         * Assume the template has correct number and positions of operands.
         * Template is String with special markers. In the list below, n represents token position (1,2,3,etc)
         * in source statement (operator is token 0, parentheses count but commas don't):
         *
         * - RGn means substitute register found in n'th token of source statement.
         * - LLn means substitute low order 16 bits from label address in source token n.
         * - LHn means substitute high order 16 bits from label address in source token n. Must add 1 if address bit 15 is 1.
         * - PCLn is similar to LLn except the value substituted will be relative to PC of the psuedo-op that generated it.
         * - PCHn is similar to LHn except the value substituted will be relative to PC of the psuedo-op that generated it.
         * - VLn means substitute low order 16 bits from 32 bit value in source token n.
         * - VHn means substitute high order 16 bits from 32 bit value in source token n, then add 1 if value's bit 15 is 1.
         * - LAB means substitute textual label from last token of source statement. Used for various branches.
         *
         *
         * @param template
         * a String containing template for basic statement.
         * @param tokenList
         * a TokenList containing tokens from extended instruction.
         * @param program
         * a [RISCVProgram] object
         * @param pc
         * an int
         * @return String representing basic assembler statement.
         */
        fun makeTemplateSubstitutions(
            program: RISCVProgram,
            template: String,
            tokenList: TokenList,
            pc: Int
        ): String {
            var instruction = template
            // substitute first operand token for template's RG1 or OP1, second for RG2 or
            // OP2, etc
            for (op in 1..<tokenList.size) {
                instruction = instruction.replace("RG$op", tokenList.get(op).text)

                val strValue = tokenList.get(op).text
                val value = strValue.translateToInt()
                if (value == null) {
                    val lval = strValue.translateToLong() ?: continue
                    val shifted = (lval shr 32).toInt()
                    val vall = lval.toInt()
                    // this shouldn't happen if it is for LL...VH
                    when {
                        "LIA$op" in instruction -> {
                            // add extra to compensate for sign extension
                            val extra = BinaryUtilsOld.bitValue(shifted, 11)
                            instruction = instruction.replace("LIA$op", ((shifted shr 12) + extra).toString())
                        }
                        "LIB$op" in instruction -> {
                            instruction = instruction.replace("LIB$op", (shifted shl 20 shr 20).toString())
                        }
                        "LIC$op" in instruction -> {
                            instruction = instruction.replace("LIC$op", ((vall shr 21) and 0x7FF).toString())
                        }
                        "LID$op" in instruction -> {
                            instruction = instruction.replace("LID$op", ((vall shr 10) and 0x7FF).toString())
                        }
                        "LIE$op" in instruction -> {
                            instruction = instruction.replace("LIE$op", (vall and 0x3FF).toString())
                        }
                    }
                    continue
                }

                val relative = value - pc
                if (instruction.contains("PCH$op")) {
                    val extra = BinaryUtilsOld.bitValue(relative, 11) // add extra to compesate for sign extension
                    instruction = instruction.replace("PCH$op", ((relative shr 12) + extra).toString())
                }
                if (instruction.contains("PCL$op")) {
                    instruction = instruction.replace("PCL$op", (relative shl 20 shr 20).toString())
                }

                if (instruction.contains("LH$op")) {
                    val extra = BinaryUtilsOld.bitValue(value, 11) // add extra to compesate for sign extension
                    //                instruction = ExtendedInstruction.substitute(instruction, "LH" + op, String.valueOf((val >> 12) + 
//                extra));
                    instruction = instruction.replace("LH$op", ((value shr 12) + extra).toString())
                }
                if (instruction.contains("LL$op")) {
                    instruction = instruction.replace("LL$op", (value shl 20 shr 20).toString())
                }

                if (instruction.contains("VH$op")) {
                    val extra = BinaryUtilsOld.bitValue(value, 11) // add extra to compesate for sign extension
                    instruction = instruction.replace("VH$op", ((value shr 12) + extra).toString())
                }
                if (instruction.contains("VL$op")) {
                    instruction = instruction.replace("VL$op", (value shl 20 shr 20).toString())
                }
            }
            // substitute label if necessary
            if (instruction.contains("LAB")) {
                // label has to be last token. It has already been translated to address
                // by symtab lookup, so I need to get the text label back so parseLine() won't
                // puke.
                val label = tokenList.get(tokenList.size - 1).text
                val sym = program.localSymbolTable!!.getSymbolGivenAddressLocalOrGlobal(label)
                if (sym != null) {
                    // should never be null, since there would not be an address if label were not
                    // in symtab!
                    // DPS 9 Dec 2007: The "substitute()" method will substitute for ALL matches.
                    // Here
                    // we want to substitute only for the first match, for two reasons: (1) a
                    // statement
                    // can only contain one label reference, its last operand, and (2) If the user's
                    // label
                    // contains the substring "LAB", then substitute() will go into an infinite loop
                    // because
                    // it will keep matching the substituted string!
//                instruction = ExtendedInstruction.substituteFirst(instruction, "LAB", sym.name);
                    instruction = instruction.replace("LAB", sym.name)
                }
            }
            return instruction
        }

        private fun buildTranslationList(translation: String): List<String> {
            require(translation.isNotEmpty()) { "Translation string cannot be empty" }
            val translationList = mutableListOf<String>()
            val st = StringTokenizer(translation, "\n")
            while (st.hasMoreTokens()) {
                translationList.add(st.nextToken())
            }
            return translationList
        }
    }
}
