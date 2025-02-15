package rars.riscv;

import org.jetbrains.annotations.NotNull;

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
     * Characters used in instruction mask to indicate bit positions
     * for 'f'irst, 's'econd, 't'hird, 'q'uad, and 'p'enta operands .
     **/
    public static final char[] operandMask = {'f', 's', 't', 'q', 'p'};
    /**
     * The instruction name.
     **/
    public final @NotNull String mnemonic;
    /**
     * Example usage of this instruction. Is provided as subclass constructor
     * argument.
     **/
    public final @NotNull String exampleFormat;
    /**
     * Description of instruction for display to user
     **/
    public final @NotNull String description;

    protected Instruction(final @NotNull String example, final @NotNull String description) {
        this.mnemonic = extractOperator(example);
        this.exampleFormat = example;
        this.description = description;
    }

    /**
     * Used by subclass constructors to extract operator mnemonic from the
     * instruction example.
     *
     * @param example
     *     a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    protected static String extractOperator(final @NotNull String example) {
        final StringTokenizer st = new StringTokenizer(example, " ,\t");
        return st.nextToken();
    }

    public abstract int getInstructionLength();
}
