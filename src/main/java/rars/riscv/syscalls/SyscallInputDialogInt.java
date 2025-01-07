package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;

import javax.swing.*;

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
 * Service to input data.
 * <p>
 * Input arguments: a0 = address of null-terminated string that is the message
 * to user<br>
 * Outputs:<br>
 * a0 contains value of int read <br>
 * a1 contains status value <br>
 * 0: valid input data, correctly parsed <br>
 * -1: input data cannot be correctly parsed <br>
 * -2: Cancel was chosen <br>
 * -3: OK was chosen but no data had been input into field <br>
 */
public final class SyscallInputDialogInt extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallInputDialogInt.</p>
     */
    public SyscallInputDialogInt() {
        super("InputDialogInt");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        final String message = NullString.get(statement);

        // Values returned by Java's InputDialog:
        // A null return value means that "Cancel" was chosen rather than OK.
        // An empty string returned (that is, inputValue.length() of zero)
        // means that OK was chosen but no string was input.
        final String inputValue = JOptionPane.showInputDialog(message);
        if (inputValue == null) // Cancel was chosen
        {
            Globals.REGISTER_FILE.updateRegisterByName("a0", 0);
            Globals.REGISTER_FILE.updateRegisterByName("a1", -2);
        } else if (inputValue.isEmpty()) // OK was chosen but there was no input
        {
            Globals.REGISTER_FILE.updateRegisterByName("a0", 0);
            Globals.REGISTER_FILE.updateRegisterByName("a1", -3);
        } else {
            try {
                final int i = Integer.parseInt(inputValue);

                // Successful parse of valid input data
                // set to the data read
                Globals.REGISTER_FILE.updateRegisterByName("a0", i);
                // set to valid flag
                Globals.REGISTER_FILE.updateRegisterByName("a1", 0);
            } catch (final NumberFormatException e) {
                // Unsuccessful parse of input data
                Globals.REGISTER_FILE.updateRegisterByName("a0", 0);
                Globals.REGISTER_FILE.updateRegisterByName("a1", -1);

            }

        } // end else

    }

}
