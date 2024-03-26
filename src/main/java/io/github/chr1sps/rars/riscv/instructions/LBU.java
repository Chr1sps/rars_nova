package io.github.chr1sps.rars.riscv.instructions;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.exceptions.AddressErrorException;

/*
Copyright (c) 2017,  Benjamin Landers

Developed by Benjamin Landers (benjaminrlanders@gmail.com)

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
 * <p>LBU class.</p>
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class LBU extends Load {
    /**
     * <p>Constructor for LBU.</p>
     */
    public LBU() {
        super("lbu t1, -100(t2)", "Set t1 to zero-extended 8-bit value from effective memory byte address", "100");
    }

    /**
     * {@inheritDoc}
     */
    public long load(int address) throws AddressErrorException {
        return Globals.memory.getByte(address) & 0x000000FF;
    }
}
