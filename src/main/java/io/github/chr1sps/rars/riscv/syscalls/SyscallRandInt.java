package io.github.chr1sps.rars.riscv.syscalls;

import io.github.chr1sps.rars.ProgramStatement;
import io.github.chr1sps.rars.riscv.AbstractSyscall;
import io.github.chr1sps.rars.riscv.hardware.RegisterFile;

import java.util.Random;

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
 * <p>SyscallRandInt class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SyscallRandInt extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallRandInt.</p>
     */
    public SyscallRandInt() {
        super("RandInt", "Get a random integer", "a0 = index of pseudorandom number generator", "a0 = random integer");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        Random stream = RandomStreams.get("a0");
        RegisterFile.updateRegister("a0", stream.nextInt());
    }
}
