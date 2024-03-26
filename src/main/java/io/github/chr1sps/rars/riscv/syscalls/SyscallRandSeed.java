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
 * <p>SyscallRandSeed class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class SyscallRandSeed extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallRandSeed.</p>
     */
    public SyscallRandSeed() {
        super("RandSeed", "Set seed for the underlying Java pseudorandom number generator",
                "a0 = index of pseudorandom number generator<br>a1 = the seed", "N/A");
    }

    /**
     * {@inheritDoc}
     */
    public void simulate(ProgramStatement statement) {
        Integer index = RegisterFile.getValue("a0");
        Random stream = RandomStreams.randomStreams.get(index);
        if (stream == null) {
            RandomStreams.randomStreams.put(index, new Random(RegisterFile.getValue("a1")));
        } else {
            stream.setSeed(RegisterFile.getValue("a1"));
        }
    }
}
