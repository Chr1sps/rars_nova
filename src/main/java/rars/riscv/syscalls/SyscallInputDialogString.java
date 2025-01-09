package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;

import javax.swing.*;
import java.nio.charset.StandardCharsets;

import static rars.Globals.MEMORY_INSTANCE;
import static rars.Globals.REGISTER_FILE;

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
 * Input arguments:<br>
 * a0 = address of null-terminated string that is the message to user <br>
 * a1 = address of input buffer for the input string <br>
 * a2 = maximum number of characters to read <br>
 * Outputs:<br>
 * a1 contains status value <br>
 * 0: valid input data, correctly parsed <br>
 * -1: input data cannot be correctly parsed <br>
 * -2: Cancel was chosen <br>
 * -3: OK was chosen but no data had been input into field <br>
 */
public final class SyscallInputDialogString extends AbstractSyscall {
    public SyscallInputDialogString() {
        super(
            "InputDialogString", "Service to display a message to a user and request a string input",
            "a0 = address of null-terminated string that is the message to user<br>a1 = address of input buffer<br>"
                +
                "a2 = maximum number of characters to read (including the terminating null)",
            "a1 contains status value.<br> 0: OK status. Buffer contains the input string.<br>-2: Cancel was " +
                "chosen. No change to buffer.<br>"
                +
                "-3: OK was chosen but no data had been input into field. No change to buffer.<br>-4: length " +
                "of the input string exceeded the specified maximum. Buffer contains the maximum allowable " +
                "input string terminated with null."
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        final String message = NullString.get(statement);

        // Values returned by Java's InputDialog:
        // A null return value means that "Cancel" was chosen rather than OK.
        // An empty string returned (that is, inputString.length() of zero)
        // means that OK was chosen but no string was input.
        final String inputString = JOptionPane.showInputDialog(message);
        final int byteAddress = REGISTER_FILE.getIntValue("a1"); // byteAddress of string is in a1
        final int maxLength = REGISTER_FILE.getIntValue("a2"); // input buffer size for input string is in a2

        try {
            if (inputString == null) // Cancel was chosen
            {
                REGISTER_FILE.updateRegisterByName("a1", -2);
            } else if (inputString.isEmpty()) // OK was chosen but there was no input
            {
                REGISTER_FILE.updateRegisterByName("a1", -3);
            } else {
                final byte[] utf8BytesList = inputString.getBytes(StandardCharsets.UTF_8);
                // The buffer will contain characters, a '\n' character, and the null character
                // Copy the input data to buffer as space permits
                int stringLength = Math.min(maxLength - 1, utf8BytesList.length);
                for (int index = 0; index < stringLength; index++) {
                    MEMORY_INSTANCE.setByte(
                        byteAddress + index,
                        utf8BytesList[index]
                    );
                }
                if (stringLength < maxLength - 1) {
                    MEMORY_INSTANCE.setByte(byteAddress + stringLength, '\n');
                    stringLength++;
                }
                MEMORY_INSTANCE.setByte(byteAddress + stringLength, 0);

                if (utf8BytesList.length > maxLength - 1) {
                    // length of the input string exceeded the specified maximum
                    REGISTER_FILE.updateRegisterByName("a1", -4);
                } else {
                    REGISTER_FILE.updateRegisterByName("a1", 0);
                }
            } // end else

        } // end try
        catch (final AddressErrorException e) {
            throw new ExitingException(statement, e);
        }

    }

}
