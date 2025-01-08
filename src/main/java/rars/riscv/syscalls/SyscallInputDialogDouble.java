package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
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
 */
// TODO: Fill in desc, in and out for all input dialogs
public final class SyscallInputDialogDouble extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    public SyscallInputDialogDouble() {
        super("InputDialogDouble");
    }

    /**
     * {@inheritDoc}
     * <p>
     * System call to input data.
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        // Input arguments: $a0 = address of null-terminated string that is the message
        // to user
        // Outputs:
        // fa0 value of double read. $f1 contains high order word of the double.
        // $a1 contains status value
        // 0: valid input data, correctly parsed
        // -1: input data cannot be correctly parsed
        // -2: Cancel was chosen
        // -3: OK was chosen but no data had been input into field

        String message = ""; // = "";
        int byteAddress = Globals.REGISTER_FILE.getIntValue(4);
        try {
            // Need an array to convert to String
            final char[] ch = {' '};
            ch[0] = (char) Globals.MEMORY_INSTANCE.getByte(byteAddress);
            while (ch[0] != 0) // only uses single location ch[0]
            {
                message = message.concat(new String(ch)); // parameter to String constructor is a char[] array
                byteAddress++;
                ch[0] = (char) Globals.MEMORY_INSTANCE.getByte(byteAddress);
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(statement, e);
        }

        // Values returned by Java's InputDialog:
        // A null return value means that "Cancel" was chosen rather than OK.
        // An empty string returned (that is, inputValue.length() of zero)
        // means that OK was chosen but no string was input.
        final String inputValue = JOptionPane.showInputDialog(message);

        try {
            Globals.FP_REGISTER_FILE.updateRegister(Globals.FP_REGISTER_FILE.ft0, 0);
            if (inputValue == null) // Cancel was chosen
            {
                // set $a1 to -2 flag
                Globals.REGISTER_FILE.updateRegisterByName("a1", -2);
            } else if (inputValue.isEmpty()) // OK was chosen but there was no input
            {
                // set $a1 to -3 flag
                Globals.REGISTER_FILE.updateRegisterByName("a1", -3);
            } else {
                final double doubleValue = Double.parseDouble(inputValue);

                // Successful parse of valid input data
                Globals.FP_REGISTER_FILE.updateRegister(
                    Globals.FP_REGISTER_FILE.fa0,
                    Double.doubleToRawLongBits(doubleValue)
                );
                // set $a1 to valid flag
                Globals.REGISTER_FILE.updateRegisterByName("a1", 0);

            }
        } catch (final
        NumberFormatException e) // Unsuccessful parse of input data
        {
            // set $a1 to -1 flag
            Globals.REGISTER_FILE.updateRegisterByName("a1", -1);
        }
    }
}
