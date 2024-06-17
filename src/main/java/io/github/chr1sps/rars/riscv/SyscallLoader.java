package io.github.chr1sps.rars.riscv;

import io.github.chr1sps.rars.Globals;
import io.github.chr1sps.rars.util.FilenameFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;

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
 * This class provides functionality to bring external Syscall definitions
 * into RARS. This permits anyone with knowledge of the Rars public interfaces,
 * in particular of the Memory and Register classes, to write custom RISCV
 * syscall
 * functions. This is adapted from the ToolLoader class, which is in turn
 * adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */
public class SyscallLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CLASS_PREFIX = "io.github.chr1sps.rars.riscv.syscalls.";
    private static final String SYSCALLS_DIRECTORY_PATH = "io/github/chr1sps/rars/riscv/syscalls";
    private static final String CLASS_EXTENSION = "class";

    private static ArrayList<AbstractSyscall> syscallList;

    /*
     * Dynamically loads Syscalls into an ArrayList. This method is adapted from
     * the loadGameControllers() method in Bret Barker's GameServer class.
     * Barker (bret@hypefiend.com) is co-author of the book "Developing Games
     * in Java".  Also see the "loadMarsTools()" method from ToolLoader class.
     */
    static {
        SyscallLoader.syscallList = new ArrayList<>();
        // grab all class files in the same directory as Syscall
        final ArrayList<String> candidates = FilenameFinder.getFilenameList(SyscallLoader.class.getClassLoader(),
                SyscallLoader.SYSCALLS_DIRECTORY_PATH, SyscallLoader.CLASS_EXTENSION);
        final HashSet<String> syscalls = new HashSet<>();
        for (final String file : candidates) {
            // Do not add class if already encountered (happens if run in MARS development
            // directory)
            if (syscalls.contains(file)) {
                continue;
            } else {
                syscalls.add(file);
            }
            try {
                // grab the class, make sure it implements Syscall, instantiate, add to list
                final String syscallClassName = SyscallLoader.CLASS_PREFIX + file.substring(0, file.indexOf(SyscallLoader.CLASS_EXTENSION) - 1);
                final Class<?> clas = Class.forName(syscallClassName);
                if (!AbstractSyscall.class.isAssignableFrom(clas)) {
                    continue;
                }
                final AbstractSyscall syscall = (AbstractSyscall) clas.getDeclaredConstructor().newInstance();
                if (syscall.getNumber() == -1) {
                    SyscallLoader.syscallList.add(syscall);
                } else {
                    throw new Exception("Syscalls must assign -1 for number");
                }
            } catch (final Exception e) {
                SyscallLoader.LOGGER.fatal("Error instantiating Syscall from file {}: {}", file, e);
                System.exit(0);
            }
        }
        SyscallLoader.syscallList = SyscallLoader.processSyscallNumberOverrides(SyscallLoader.syscallList);
    }

    // Loads system call numbers from Syscall.properties
    private static ArrayList<AbstractSyscall> processSyscallNumberOverrides(final ArrayList<AbstractSyscall> syscallList) {
        final ArrayList<SyscallNumberOverride> overrides = new Globals().getSyscallOverrides();
        if (syscallList.size() != overrides.size()) {
            SyscallLoader.LOGGER.fatal(
                    "Error: the number of entries in the config file does not match the number of syscalls loaded");
            SyscallLoader.LOGGER.fatal(
                    "Ensure there is a Syscall.properties file in the directory you are executing if you are a developer");
            SyscallLoader.LOGGER.fatal("syscall list: {}, overrides: {}", syscallList.size(), overrides.size());
            System.exit(0);
        }
        for (final SyscallNumberOverride override : overrides) {
            boolean match = false;
            for (final AbstractSyscall syscall : syscallList) {
                if (syscall.getNumber() == override.getNumber()) {
                    SyscallLoader.LOGGER.fatal("Duplicate service number: {} already registered to {}", syscall.getNumber(), SyscallLoader.findSyscall(syscall.getNumber()).getName());
                    System.exit(0);
                }
                if (override.getName().equals(syscall.getName())) {
                    if (syscall.getNumber() != -1) {
                        SyscallLoader.LOGGER.fatal("Error: {} was assigned a numebr twice in the config file", syscall.getName());
                        System.exit(0);
                    }
                    if (override.getNumber() < 0) {
                        SyscallLoader.LOGGER.fatal("Error: {} was assigned a negative number", override.getName());
                        System.exit(0);
                    }
                    // we have a match to service name, assign new number
                    syscall.setNumber(override.getNumber());
                    match = true;
                }
            }
            if (!match) {
                SyscallLoader.LOGGER.fatal("Error: syscall name '{}' in config file does not match any name in syscall list", override.getName());
                System.exit(0);
            }
        }
        return syscallList;
    }

    /*
     * Method to find Syscall object associated with given service number.
     * Returns null if no associated object found.
     */

    /**
     * <p>findSyscall.</p>
     *
     * @param number a int
     * @return a {@link io.github.chr1sps.rars.riscv.AbstractSyscall} object
     */
    public static AbstractSyscall findSyscall(final int number) {
        // linear search is OK since number of syscalls is small.
        for (final AbstractSyscall service : SyscallLoader.syscallList) {
            if (service.getNumber() == number) {
                return service;
            }
        }
        return null;
    }

    /**
     * <p>Getter for the field <code>syscallList</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public static ArrayList<AbstractSyscall> getSyscallList() {
        return SyscallLoader.syscallList;
    }
}
