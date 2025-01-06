package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.exceptions.ExitingException;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.RegisterFile;
import rars.util.SystemIO;

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
 * <p>SyscallOpen class.</p>
 */
public final class SyscallOpen extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallOpen.</p>
     */
    public SyscallOpen() {
        super(
            "Open", "Opens a file from a path <br>Only supported flags (a1) are read-only (0), write-only (1) and" +
                " write-append (9). write-only flag creates file if it does not exist, so it is technically" +
                " write-create.  write-append will start writing at end of existing file.",
            "a0 = Null terminated string for the path <br>a1 = flags",
            "a0 = the file decriptor or -1 if an error occurred"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) throws ExitingException {
        final int retValue = SystemIO.openFile(
            NullString.get(statement),
            RegisterFile.INSTANCE.getIntValue("a1")
        );
        // set returned fd second in register
        RegisterFile.INSTANCE.updateRegisterByName("a0", retValue);
    }
}
