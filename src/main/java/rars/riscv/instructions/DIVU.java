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

public final class DIVU extends Arithmetic {
    public static final @NotNull DIVU INSTANCE = new DIVU();

    private DIVU() {
        super(
            "divu t1,t2,t3", "Division: set t1 to the result of t2/t3 using unsigned division",
            "0000001", "101"
        );
    }

    @Override
    public long compute(final long value, final long value2) {
        // Signal illegal division with -1
        if (value2 == 0) {
            return -1;
        }
        return Long.divideUnsigned(value, value2);
    }

    @Override
    public int computeW(final int value, final int value2) {
        return (int) this.compute(value & 0xFFFFFFFFL, value2 & 0xFFFFFFFFL);
    }
}
