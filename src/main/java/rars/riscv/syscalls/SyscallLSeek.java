package rars.riscv.syscalls;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.ProgramStatement;
import rars.riscv.AbstractSyscall;
import rars.util.SystemIO;

/*
Copyright (c) 2017, Benjamin Landers

Developed by Benjamin Landers (benjminrlanders@gmail.com)

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
 * <p>SyscallLSeek class.</p>
 */
public final class SyscallLSeek extends AbstractSyscall {
    /**
     * <p>Constructor for SyscallLSeek.</p>
     */
    public SyscallLSeek() {
        super(
            "LSeek", "Seek to a position in a file",
            "a0 = the file descriptor <br> a1 = the offset for the base <br>a2 is the begining of the file (0)," +
                " the current position (1), or the end of the file (2)}",
            "a0 = the selected position from the beginning of the file or -1 is an error occurred"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void simulate(final @NotNull ProgramStatement statement) {
        final int result = SystemIO.seek(
            Globals.REGISTER_FILE.getIntValue("a0"),
            Globals.REGISTER_FILE.getIntValue("a1"),
            Globals.REGISTER_FILE.getIntValue("a2")
        );
        Globals.REGISTER_FILE.updateRegisterByName("a0", result);
    }
}
