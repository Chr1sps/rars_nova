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

public class SyscallRead extends AbstractSyscall {
    public SyscallRead() {
        super(
            "Read", "Read from a file descriptor into a buffer",
            "a0 = the file descriptor <br>a1 = address of the buffer <br>a2 = maximum length to read",
            "a0 = the length read or -1 if error"
        );
    }

    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        int byteAddress = RegisterFile.INSTANCE.getIntValue("a1"); // destination of characters read from file
        int index = 0;
        final int length = RegisterFile.INSTANCE.getIntValue("a2");
        final byte[] myBuffer = new byte[length]; // specified length
        // Call to SystemIO.xxxx.read(xxx,xxx,xxx) returns actual length
        final int retLength = SystemIO.readFromFile(
            RegisterFile.INSTANCE.getIntValue("a0"), // fd
            myBuffer, // buffer
            length
        ); // length
        // set returned value in register
        RegisterFile.INSTANCE.updateRegisterByName("a0", retLength);

        // copy bytes from returned buffer into memory
        try {
            while (index < retLength) {
                Globals.MEMORY_INSTANCE.setByte(
                    byteAddress++,
                    myBuffer[index++]
                );
            }
        } catch (final AddressErrorException e) {
            throw new ExitingException(statement, e);
        }
    }
}
