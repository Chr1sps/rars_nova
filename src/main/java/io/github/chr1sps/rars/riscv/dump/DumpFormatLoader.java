package io.github.chr1sps.rars.riscv.dump;

import io.github.chr1sps.rars.util.FilenameFinder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

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
 *
 * @author chrisps
 * @version $Id: $Id
 */
public class DumpFormatLoader {

    private static final String CLASS_PREFIX = "com.chrisps.rars.riscv.dump.";
    private static final String DUMP_DIRECTORY_PATH = "com/chrisps/rars/riscv/dump";
    private static final String CLASS_EXTENSION = "class";

    private static ArrayList<DumpFormat> formatList = null;

    static {
        formatList = new ArrayList<>();
        // grab all class files in the dump directory
        ArrayList<String> candidates = FilenameFinder.getFilenameList(DumpFormatLoader.class.getClassLoader(),
                DUMP_DIRECTORY_PATH, CLASS_EXTENSION);
        for (String file : candidates) {
            try {
                // grab the class, make sure it implements DumpFormat, instantiate, add to list
                String formatClassName = CLASS_PREFIX + file.substring(0, file.indexOf(CLASS_EXTENSION) - 1);
                Class clas = Class.forName(formatClassName);
                if (DumpFormat.class.isAssignableFrom(clas) &&
                        !Modifier.isAbstract(clas.getModifiers()) &&
                        !Modifier.isInterface(clas.getModifiers())) {
                    formatList.add((DumpFormat) clas.newInstance());
                }
            } catch (Exception e) {
                System.out.println("Error instantiating DumpFormat from file " + file + ": " + e);
            }
        }
    }

    /**
     * Dynamically loads dump formats into an ArrayList. This method is adapted from
     * the loadGameControllers() method in Bret Barker's GameServer class.
     * Barker (bret@hypefiend.com) is co-author of the book "Developing Games
     * in Java".
     *
     * @return a {@link java.util.ArrayList} object
     * @see io.github.chr1sps.rars.riscv.SyscallLoader
     * @see io.github.chr1sps.rars.venus.ToolLoader
     * @see io.github.chr1sps.rars.riscv.InstructionSet
     */
    public static ArrayList<DumpFormat> getDumpFormats() {
        return formatList;
    }

    /**
     * <p>findDumpFormatGivenCommandDescriptor.</p>
     *
     * @param formatCommandDescriptor a {@link java.lang.String} object
     * @return a {@link io.github.chr1sps.rars.riscv.dump.DumpFormat} object
     */
    public static DumpFormat findDumpFormatGivenCommandDescriptor(String formatCommandDescriptor) {
        for (DumpFormat f : formatList) {
            if (f.getCommandDescriptor().equals(formatCommandDescriptor)) {
                return f;
            }
        }
        return null;
    }

}
