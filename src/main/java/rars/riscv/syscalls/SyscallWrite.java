package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.RegisterFile;
import rars.util.SystemIO;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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
 * Service to write to file descriptor given in a0. a1 specifies buffer
 * and a2 specifies length. Number of characters written is returned in a0.
 */
public class SyscallWrite extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallWrite.</p>
     */
    public SyscallWrite() {
        super(
                "Write", "Write to a filedescriptor from a buffer",
                "a0 = the file descriptor<br>a1 = the buffer address<br>a2 = the length to write",
                "a0 = the number of charcters written"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        int byteAddress = RegisterFile.getValue("a1"); // source of characters to write to file
        final int reqLength = RegisterFile.getValue("a2"); // user-requested length
        if (reqLength < 0) {
            RegisterFile.updateRegister("a0", -1);
            return;
        }
        int index = 0;
        final byte[] myBuffer = new byte[reqLength];
        try {
            var byteValue = Globals.MEMORY_INSTANCE.getByte(byteAddress);
            while (index < reqLength) // Stop at requested length. Null bytes are included.
            {
                myBuffer[index++] = byteValue;
                byteAddress++;
                byteValue = Globals.MEMORY_INSTANCE.getByte(byteAddress);
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(statement, e);
        }
        final int retValue = SystemIO.writeToFile(
                RegisterFile.getValue("a0"), // fd
                myBuffer, // buffer
                RegisterFile.getValue("a2")
        ); // length
        RegisterFile.updateRegister("a0", retValue); // set returned second in register
    }
}
