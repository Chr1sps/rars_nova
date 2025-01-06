package rars.simulator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import rars.Globals;
import rars.assembler.DataTypes;
import rars.exceptions.AddressErrorException;
import rars.riscv.hardware.RegisterFile;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
 * Models Program Arguments, one or more strings provided to the source
 * program at runtime. Equivalent to C's main(int argc, char **argv) or
 * Java's main(String[] args).
 *
 * @param programArgumentList
 *     list of strings, each element containing one argument
 * @author Pete Sanderson
 * @version July 2008
 */
public record ProgramArgumentList(@NotNull @Unmodifiable List<@NotNull String> programArgumentList) {
    private static final @NotNull Logger LOGGER = LogManager.getLogger(ProgramArgumentList.class);

    /**
     * Constructor that parses string to produce list. Delimiters
     * are the default Java StringTokenizer delimiters (space, tab,
     * newline, return, formfeed)
     *
     * @param args
     *     String containing delimiter-separated arguments
     */
    public ProgramArgumentList(final @NotNull String args) {
        this(buildArgsFromString(args));
    }

    private static @NotNull List<@NotNull String> buildArgsFromString(final @NotNull String args) {
        final StringTokenizer st = new StringTokenizer(args);
        final var resultList = new ArrayList<@NotNull String>(st.countTokens());
        while (st.hasMoreTokens()) {
            resultList.add(st.nextToken());
        }
        return resultList;
    }

    /**
     * Place any program arguments into memory and registers
     * Arguments are stored starting at highest word of non-kernel
     * memory and working back toward runtime stack (there is a 4096
     * byte gap in between). The argument count (argc) and pointers
     * to the arguments are stored on the runtime stack. The stack
     * pointer register $sp is adjusted accordingly and $a0 is set
     * to the argument count (argc), and $a1 is set to the stack
     * address holding the first argument pointer (argv).
     */
    public void storeProgramArguments() {
        if (this.programArgumentList.isEmpty()) {
            return;
        }
        // Runtime stack initialization from stack top-down (each is 4 bytes) :
        // programArgumentList.size()
        // address of first character of first program argument
        // address of first character of second program argument
        // ....repeat for all program arguments
        // 0x00000000 (null terminator for list of string pointers)
        // $sp will be set to the address holding the arg list size
        // $a0 will be set to the arg list size (argc)
        // $a1 will be set to stack address just "below" arg list size (argv)
        // Each of the arguments themselves will be stored starting at
        // Memory.stackBaseAddress (0x7ffffffc) and working down from there:
        // 0x7ffffffc will contain null terminator for first arg
        // 0x7ffffffb will contain last character of first arg
        // 0x7ffffffa will contain next-to-last character of first arg
        // Etc down to first character of first arg.
        // Previous address will contain null terminator for second arg
        // Previous-to-that contains last character of second arg
        // Etc down to first character of second arg.
        // Follow this pattern for all remaining arguments.

        final var memoryConfiguration = Globals.MEMORY_INSTANCE.getMemoryConfiguration();
        int highAddress = memoryConfiguration.stackBaseAddress; // highest non-kernel address, sits "under" stack
        final int[] argStartAddress = new int[this.programArgumentList.size()];
        try { // needed for all memory writes
            for (int i = 0; i < this.programArgumentList.size(); i++) {
                final var programArgument = this.programArgumentList.get(i);
                Globals.MEMORY_INSTANCE.set(highAddress, 0, 1); // trailing null byte for each argument
                highAddress--;
                for (int j = programArgument.length() - 1; j >= 0; j--) {
                    Globals.MEMORY_INSTANCE.set(highAddress, programArgument.charAt(j), 1);
                    highAddress--;
                }
                argStartAddress[i] = highAddress + 1;
            }
            // now place a null word, the arg starting addresses, and arg count onto stack.
            int stackAddress = memoryConfiguration.stackPointerAddress; // base address for runtime stack.
            if (highAddress < memoryConfiguration.stackPointerAddress) {
                // Based on current values for stackBaseAddress and stackPointer, this will
                // only happen if the combined lengths of program arguments is greater than
                // 0x7ffffffc - 0x7fffeffc = 0x00001000 = 4096 bytes. In this case, set
                // stackAddress to next lower word boundary minus 4 for clearance (since every
                // byte from highAddress+1 is filled).
                stackAddress = highAddress - (highAddress % DataTypes.WORD_SIZE) - DataTypes.WORD_SIZE;
            }
            Globals.MEMORY_INSTANCE.set(stackAddress, 0, DataTypes.WORD_SIZE); // null word for end of argv array
            stackAddress -= DataTypes.WORD_SIZE;
            for (int i = argStartAddress.length - 1; i >= 0; i--) {
                Globals.MEMORY_INSTANCE.set(stackAddress, argStartAddress[i], DataTypes.WORD_SIZE);
                stackAddress -= DataTypes.WORD_SIZE;
            }
            Globals.MEMORY_INSTANCE.set(stackAddress, argStartAddress.length, DataTypes.WORD_SIZE); // argc
            stackAddress -= DataTypes.WORD_SIZE;

            // Need to set $sp register to stack address, $a0 to argc, $a1 to argv
            // Need to by-pass the backstepping mechanism so go directly to Register instead
            // of RegisterFile
            RegisterFile.INSTANCE.sp.setValue(stackAddress + DataTypes.WORD_SIZE);
            RegisterFile.INSTANCE.a0.setValue(argStartAddress.length); // argc
            RegisterFile.INSTANCE.a1.setValue(stackAddress + DataTypes.WORD_SIZE + DataTypes.WORD_SIZE); // argv
        } catch (final AddressErrorException aee) {
            ProgramArgumentList.LOGGER.fatal(
                "Internal Error: Memory write error occurred while storing program " +
                    "arguments!", aee
            );
            System.exit(0);
        }
    }
}
