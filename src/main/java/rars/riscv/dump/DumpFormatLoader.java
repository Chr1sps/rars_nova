package rars.riscv.dump;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rars.riscv.InstructionSet;
import rars.riscv.SyscallLoader;
import rars.riscv.dump.formats.*;
import rars.venus.ToolLoader;

import java.util.List;

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

/* This class provides functionality to bring external memory dump format definitions
 * into RARS.  This is adapted from the ToolLoader class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */

/**
 * <p>DumpFormatLoader class.</p>
 */
public final class DumpFormatLoader {
    private static final List<DumpFormat> formatList = List.of(
            new AsciiTextDumpFormat(),
            new BinaryDumpFormat(),
            new BinaryTextDumpFormat(),
            new HexTextDumpFormat(),
            new IntelHexDumpFormat(),
            new SegmentWindowDumpFormat()
    );

    private DumpFormatLoader() {
    }


    /**
     * Dynamically loads dump formats into an ArrayList. This method is adapted from
     * the loadGameControllers() method in Bret Barker's GameServer class.
     * Barker (bret@hypefiend.com) is co-author of the book "Developing Games
     * in Java".
     *
     * @return a {@link java.util.ArrayList} object
     * @see SyscallLoader
     * @see ToolLoader
     * @see InstructionSet
     */
    public static @NotNull List<DumpFormat> getDumpFormats() {
        return DumpFormatLoader.formatList;
    }

    /**
     * <p>findDumpFormatGivenCommandDescriptor.</p>
     *
     * @param formatCommandDescriptor a {@link java.lang.String} object
     * @return a {@link DumpFormat} object
     */
    public static @Nullable DumpFormat findDumpFormatGivenCommandDescriptor(final String formatCommandDescriptor) {
        for (final DumpFormat f : DumpFormatLoader.formatList) {
            if (f.getCommandDescriptor().equals(formatCommandDescriptor)) {
                return f;
            }
        }
        return null;
    }

}
