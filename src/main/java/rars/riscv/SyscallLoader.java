package rars.riscv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import rars.Globals;
import rars.riscv.syscalls.*;

import java.util.ArrayList;

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
public final class SyscallLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    private static ArrayList<AbstractSyscall> syscallList = new ArrayList<>();

    /*
     * Dynamically loads Syscalls into an ArrayList. This method is adapted from
     * the loadGameControllers() method in Bret Barker's GameServer class.
     * Barker (bret@hypefiend.com) is co-author of the book "Developing Games
     * in Java".  Also see the "loadMarsTools()" method from ToolLoader class.
     */
    static {
        // Replaced dynamic loading with static loading to avoid any funny loading errors.
        syscallList.add(new SyscallClose());
        syscallList.add(new SyscallConfirmDialog());
        syscallList.add(new SyscallExit());
        syscallList.add(new SyscallExit2());
        syscallList.add(new SyscallGetCWD());
        syscallList.add(new SyscallInputDialogDouble());
        syscallList.add(new SyscallInputDialogFloat());
        syscallList.add(new SyscallInputDialogInt());
        syscallList.add(new SyscallInputDialogString());
        syscallList.add(new SyscallLSeek());
        syscallList.add(new SyscallMessageDialog());
        syscallList.add(new SyscallMessageDialogDouble());
        syscallList.add(new SyscallMessageDialogFloat());
        syscallList.add(new SyscallMessageDialogInt());
        syscallList.add(new SyscallMessageDialogString());
        syscallList.add(new SyscallMidiOut());
        syscallList.add(new SyscallMidiOutSync());
        syscallList.add(new SyscallOpen());
        syscallList.add(new SyscallPrintChar());
        syscallList.add(new SyscallPrintDouble());
        syscallList.add(new SyscallPrintFloat());
        syscallList.add(new SyscallPrintInt());
        syscallList.add(new SyscallPrintIntBinary());
        syscallList.add(new SyscallPrintIntHex());
        syscallList.add(new SyscallPrintIntUnsigned());
        syscallList.add(new SyscallPrintString());
        syscallList.add(new SyscallRandDouble());
        syscallList.add(new SyscallRandFloat());
        syscallList.add(new SyscallRandInt());
        syscallList.add(new SyscallRandIntRange());
        syscallList.add(new SyscallRandSeed());
        syscallList.add(new SyscallRead());
        syscallList.add(new SyscallReadChar());
        syscallList.add(new SyscallReadDouble());
        syscallList.add(new SyscallReadFloat());
        syscallList.add(new SyscallReadInt());
        syscallList.add(new SyscallReadString());
        syscallList.add(new SyscallSbrk());
        syscallList.add(new SyscallSleep());
        syscallList.add(new SyscallTime());
        syscallList.add(new SyscallWrite());

        syscallList.add(new SyscallDisplayBitmap());

        SyscallLoader.syscallList = SyscallLoader.processSyscallNumberOverrides(SyscallLoader.syscallList);
    }

    private SyscallLoader() {
    }

    // Loads system call numbers from Syscall.properties
    private static @NotNull ArrayList<AbstractSyscall> processSyscallNumberOverrides(final @NotNull ArrayList<AbstractSyscall> syscallList) {
        final ArrayList<SyscallNumberOverride> overrides = Globals.getSyscallOverrides();
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
                if (syscall.getNumber() == override.number()) {
                    final var syscallName = syscall.getName();
                    SyscallLoader.LOGGER.fatal("Duplicate service number: {} already registered to {}", syscall.getNumber(), syscallName);
                    System.exit(0);
                }
                if (override.name().equals(syscall.getName())) {
                    if (syscall.getNumber() != -1) {
                        SyscallLoader.LOGGER.fatal("Error: {} was assigned a numebr twice in the config file", syscall.getName());
                        System.exit(0);
                    }
                    if (override.number() < 0) {
                        SyscallLoader.LOGGER.fatal("Error: {} was assigned a negative number", override.name());
                        System.exit(0);
                    }
                    // we have a match to service name, assign new number
                    syscall.setNumber(override.number());
                    match = true;
                }
            }
            if (!match) {
                SyscallLoader.LOGGER.fatal("Error: syscall name '{}' in config file does not match any name in syscall list", override.name());
                System.exit(0);
            }
        }
        return syscallList;
    }

    /**
     * Method to find Syscall object associated with given service number.
     * Returns null if no associated object found.
     *
     * @param number a int
     * @return a {@link AbstractSyscall} object
     */
    public static @Nullable AbstractSyscall findSyscall(final @Range(from = 0, to = Integer.MAX_VALUE) int number) {
        return syscallList.stream().filter(syscall -> syscall.getNumber() == number).findFirst().orElse(null);
    }

    /**
     * <p>Getter for the field <code>syscallList</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object
     */
    public static @NotNull ArrayList<AbstractSyscall> getSyscallList() {
        return SyscallLoader.syscallList;
    }
}