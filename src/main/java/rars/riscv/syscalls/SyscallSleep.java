package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.ProgramStatement;
import rars.riscv.AbstractSyscall;
import rars.riscv.hardware.RegisterFile;

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
 * <p>SyscallSleep class.</p>
 */
public class SyscallSleep extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallSleep.</p>
     */
    public SyscallSleep() {
        super("Sleep", "Set the current thread to sleep for a time (not precise)", "a0 = time to sleep in milliseconds",
                "N/A");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        try {
            Thread.sleep(RegisterFile.getValue("a0")); // units of milliseconds 1000 millisec = 1 sec.
        } catch (final InterruptedException ignored) {
        }
    }

}
