package rars.riscv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import rars.assembler.TokenList;
import rars.assembler.Tokenizer;
import rars.exceptions.AssemblyException;

import java.util.StringTokenizer;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Base class to represent member of RISCV instruction set.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003
 */
public abstract sealed class Instruction permits BasicInstruction, ExtendedInstruction {
    /**
     * Length in bytes of a machine instruction. Currently just 4 because other
     * instruction sizes defined in the specification are nor supported.
     */
    public static final int INSTRUCTION_LENGTH = 4;
    /**
     * Constant <code>INSTRUCTION_LENGTH_BITS=32</code>
     */
    public static final int INSTRUCTION_LENGTH_BITS = 32;
    /**
     * Characters used in instruction mask to indicate bit positions
     * for 'f'irst, 's'econd, 't'hird, 'q'uad, and 'p'enta operands .
     **/
    public static final char[] operandMask = {'f', 's', 't', 'q', 'p'};
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * The instruction name.
     **/
    protected String mnemonic;
    /**
     * Example usage of this instruction. Is provided as subclass constructor
     * argument.
     **/
    protected String exampleFormat;
    /**
     * Description of instruction for display to user
     **/
    protected String description;
    /**
     * List of tokens generated by tokenizing example usage (see
     * <code>exampleFormat</code>).
     **/
    protected TokenList tokenList;

    /**
     * Used by subclass constructors to extract operator mnemonic from the
     * instruction example.
     *
     * @param example a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    protected static String extractOperator(final @NotNull String example) {
        final StringTokenizer st = new StringTokenizer(example, " ,\t");
        return st.nextToken();
    }

    public String getName() {
        return this.mnemonic;
    }

    public @NotNull String getExampleFormat() {
        return this.exampleFormat;
    }

    public @NotNull String getDescription() {
        return this.description;
    }

    public @NotNull TokenList getTokenList() {
        return this.tokenList;
    }

    public int getInstructionLength() {
        return Instruction.INSTRUCTION_LENGTH;
    }

    /**
     * Used to build a token list from the example instruction
     * provided as constructor argument. Parser uses this for syntax checking.
     */
    protected void createExampleTokenList() {
        try {
            this.tokenList = ((new Tokenizer()).tokenizeExampleInstruction(this.exampleFormat));
        } catch (final AssemblyException pe) {
            Instruction.LOGGER.error("CONFIGURATION ERROR: Instruction example \"{}\" contains invalid token(s).", this.exampleFormat);
        }
    }
}
