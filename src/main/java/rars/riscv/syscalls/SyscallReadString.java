package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.AddressErrorException;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.Memory;
import rars.riscv.hardware.RegisterFile;
import rars.util.SystemIO;

import java.nio.charset.StandardCharsets;

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
 * Service to read console input string into buffer starting at address in a0
 * for a1-1 bytes.
 * <p>
 * Performs syscall function to read console input string into buffer starting
 * at address in $a0.
 * Follows semantics of UNIX 'fgets'. For specified length n,
 * string can be no longer than n-1. If less than that, add
 * newline to end. In either case, then pad with null byte.
 */
public class SyscallReadString extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallReadString.</p>
     */
    public SyscallReadString() {
        super("ReadString", "Reads a string from the console",
                "a0 = address of input buffer<br>a1 = maximum number of characters to read", "N/A");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        final String inputString;
        final int buf = RegisterFile.getValue("a0"); // buf addr
        int maxLength = RegisterFile.getValue("a1") - 1;
        boolean addNullByte = true;
        // Guard against negative maxLength. DPS 13-July-2011
        if (maxLength < 0) {
            maxLength = 0;
            addNullByte = false;
        }
        inputString = SystemIO.readString(this.getNumber(), maxLength);

        final byte[] utf8BytesList = inputString.getBytes(StandardCharsets.UTF_8);
        // TODO: allow for utf-8 encoded strings
        int stringLength = Math.min(maxLength, utf8BytesList.length);
        try {
            for (int index = 0; index < stringLength; index++) {
                Memory.getInstance().setByte(buf + index,
                        utf8BytesList[index]);
            }
            if (stringLength < maxLength) {
                Memory.getInstance().setByte(buf + stringLength, '\n');
                stringLength++;
            }
            if (addNullByte)
                Memory.getInstance().setByte(buf + stringLength, 0);
        } catch (final AddressErrorException e) {
            throw new ExitingException(statement, e);
        }
    }
}
