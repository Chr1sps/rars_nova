package rars.riscv.instructions;

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

import org.jetbrains.annotations.NotNull;

public final class SRA extends Arithmetic {
    public static final @NotNull SRA INSTANCE = new SRA();

    private SRA() {
        super(
            "sra t1,t2,t3",
            "Shift right arithmetic: Set t1 to result of sign-extended shifting t2 right by number of bits specified " +
                "by second in low-order 5 bits of t3",
            "0100000",
            "101"
        );
    }

    @Override
    public long compute(final long value, final long value2) {
        return value >> (value2 & 0b0011_1111); // Use the bottom 6 bits
    }

    @Override
    public int computeW(final int value, final int value2) {
        // Use >> to sign-fill
        return value >> (value2 & 0b0001_1111); // Only use the bottom 5 bits
    }
}
