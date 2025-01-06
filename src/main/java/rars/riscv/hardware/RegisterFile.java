package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.assembler.SymbolTable;
import rars.notices.RegisterAccessNotice;
import rars.riscv.BasicInstruction;
import rars.riscv.hardware.registers.Register;
import rars.settings.BoolSetting;
import rars.settings.OtherSettings;
import rars.util.ConversionUtils;
import rars.util.SimpleSubscriber;

import static rars.settings.BoolSettings.BOOL_SETTINGS;

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
 * Represents the collection of RISCV integer registers.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public final class RegisterFile {

    public static final int GLOBAL_POINTER_REGISTER_INDEX = 3;
    public static final int STACK_POINTER_REGISTER_INDEX = 2;
    public static final @NotNull Register
        SP_REGISTER = new Register(
        "sp",
        STACK_POINTER_REGISTER_INDEX,
        Globals.MEMORY_INSTANCE.getMemoryConfiguration().stackPointerAddress
    ),
        GP_REGISTER = new Register(
            "gp",
            GLOBAL_POINTER_REGISTER_INDEX,
            Globals.MEMORY_INSTANCE.getMemoryConfiguration().globalPointerAddress
        );
    private static final @NotNull RegisterBlock REGISTER_BLOCK = new RegisterBlock(
        'x', new Register[]{
        new Register("zero", 0, 0), new Register("ra", 1, 0),
        SP_REGISTER,
        GP_REGISTER,
        new Register("tp", 4, 0), new Register("t0", 5, 0),
        new Register("t1", 6, 0), new Register("t2", 7, 0),
        new Register("s0", 8, 0), new Register("s1", 9, 0),
        new Register("a0", 10, 0), new Register("a1", 11, 0),
        new Register("a2", 12, 0), new Register("a3", 13, 0),
        new Register("a4", 14, 0), new Register("a5", 15, 0),
        new Register("a6", 16, 0), new Register("a7", 17, 0),
        new Register("s2", 18, 0), new Register("s3", 19, 0),
        new Register("s4", 20, 0), new Register("s5", 21, 0),
        new Register("s6", 22, 0), new Register("s7", 23, 0),
        new Register("s8", 24, 0), new Register("s9", 25, 0),
        new Register("s10", 26, 0), new Register("s11", 27, 0),
        new Register("t3", 28, 0), new Register("t4", 29, 0),
        new Register("t5", 30, 0), new Register("t6", 31, 0)
    }
    );
    public static final @NotNull Register PC_REGISTER = new Register(
        "pc",
        -1,
        Globals.MEMORY_INSTANCE.getMemoryConfiguration().textBaseAddress
    );

    private RegisterFile() {
    }

    /**
     * This method updates the register value who's number is num. Also handles the
     * lo and hi registers
     *
     * @param num
     *     Register to set the value of.
     * @param val
     *     The desired value for the register.
     */
    public static void updateRegister(final int num, final long val) {
        if (num != 0) {
            if ((OtherSettings.getBackSteppingEnabled())) {
                Globals.program.getBackStepper().addRegisterFileRestore(
                    num,
                    RegisterFile.REGISTER_BLOCK.updateRegister(num, val)
                );
            } else {
                RegisterFile.REGISTER_BLOCK.updateRegister(num, val);
            }
        }
    }

    /**
     * Sets the value of the register given to the value given.
     *
     * @param name
     *     Name of register to set the value of.
     * @param val
     *     The desired value for the register.
     */
    public static void updateRegister(final String name, final long val) {
        RegisterFile.updateRegister(RegisterFile.REGISTER_BLOCK.getRegister(name).number, val);
    }

    /**
     * Returns the value of the register.
     *
     * @param num
     *     The register number.
     * @return The value of the given register.
     */
    public static int getValue(final int num) {
        return ConversionUtils.longLowerHalfToInt(RegisterFile.REGISTER_BLOCK.getValue(num));

    }

    /**
     * Returns the value of the register.
     *
     * @param num
     *     The register number.
     * @return The value of the given register.
     */
    public static long getValueLong(final int num) {
        return RegisterFile.REGISTER_BLOCK.getValue(num);

    }

    /**
     * Returns the value of the register.
     *
     * @param name
     *     The register's name.
     * @return The value of the given register.
     */
    public static int getValue(final String name) {
        return ConversionUtils.longLowerHalfToInt(RegisterFile.REGISTER_BLOCK.getValue(name));
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     */
    public static Register[] getRegisters() {
        return RegisterFile.REGISTER_BLOCK.getRegisters();
    }

    /**
     * Get register object corresponding to given name. If no match, return null.
     *
     * @param name
     *     The register name, either in $0 or $zero format.
     * @return The register object,or null if not found.
     */
    public static Register getRegister(final String name) {
        if (name.equals("fp")) {
            return RegisterFile.REGISTER_BLOCK.getRegister("s0");
        }
        return RegisterFile.REGISTER_BLOCK.getRegister(name);
    }

    /**
     * For initializing the Program Counter. Do not use this to implement jumps and
     * branches, as it will NOT record a backstep entry with the restore value.
     * If you need backstepping capability, use setProgramCounter instead.
     *
     * @param value
     *     The value to set the Program Counter to.
     */
    public static void initializeProgramCounter(final int value) {
        RegisterFile.PC_REGISTER.setValue(value);
    }

    /**
     * Will initialize the Program Counter to either the default reset value, or the
     * address
     * associated with source program global label "main", if it exists as a text
     * segment label
     * and the global setting is set.
     *
     * @param startAtMain
     *     If true, will set program counter to address of statement
     *     labeled
     *     'main' (or other defined start label) if defined. If not
     *     defined, or if parameter false,
     *     will set program counter to default reset value.
     */
    public static void initializeProgramCounter(final boolean startAtMain) {
        final int mainAddr = Globals.symbolTable.getAddress(SymbolTable.getStartLabel());
        if (startAtMain && mainAddr != SymbolTable.NOT_FOUND && Globals.MEMORY_INSTANCE.isAddressInTextSegment(mainAddr)) {
            RegisterFile.initializeProgramCounter(mainAddr);
        } else {
            RegisterFile.initializeProgramCounter((int) RegisterFile.PC_REGISTER.getResetValue());
        }
    }

    /**
     * For returning the program counters value.
     *
     * @return The program counters value as an int.
     */
    public static int getProgramCounter() {
        return (int) RegisterFile.PC_REGISTER.getValue();
    }

    /**
     * For setting the Program Counter. Note that ordinary PC update should be done
     * using
     * incrementPC() method. Use this only when processing jumps and branches.
     *
     * @param value
     *     The value to set the Program Counter to.
     */
    public static void setProgramCounter(final int value) {
        final int old = (int) RegisterFile.PC_REGISTER.getValue();
        RegisterFile.PC_REGISTER.setValue(value);
        if (OtherSettings.getBackSteppingEnabled()) {
            Globals.program.getBackStepper().addPCRestore(old);
        }
    }

    /**
     * Method to reinitialize the values of the registers.
     * <b>NOTE:</b> Should <i>not</i> be called from command-mode MARS because this
     * this method uses global settings from the registry. Command-mode must operate
     * using only the command switches, not registry settings. It can be called
     * from tools running stand-alone, and this is done in
     * {@code AbstractToolAndApplication}.
     */
    public static void resetRegisters() {
        RegisterFile.REGISTER_BLOCK.resetRegisters();
        RegisterFile.initializeProgramCounter(BOOL_SETTINGS.getSetting(BoolSetting.START_AT_MAIN));//
    }

    /**
     * Method to increment the Program counter in the general case (not a jump or
     * branch).
     */
    public static void incrementPC() {
        RegisterFile.PC_REGISTER.setValue(RegisterFile.PC_REGISTER.getValue() + BasicInstruction.BASIC_INSTRUCTION_LENGTH);
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will add the given Observer to each one. Currently does not apply to Program
     * Counter.
     *
     * @param observer
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void addRegistersObserver(final @NotNull SimpleSubscriber<? super RegisterAccessNotice> observer) {
        RegisterFile.REGISTER_BLOCK.addRegistersObserver(observer);
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will delete the given Observer from each one. Currently does not apply to
     * Program
     * Counter.
     *
     * @param observer
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void deleteRegistersObserver(final @NotNull SimpleSubscriber<? super RegisterAccessNotice> observer) {
        RegisterFile.REGISTER_BLOCK.deleteRegistersSubscriber(observer);
    }

    public static void setValuesFromConfiguration(final @NotNull MemoryConfiguration configuration) {
        RegisterFile.GP_REGISTER.changeResetValue(configuration.globalPointerAddress);
        RegisterFile.SP_REGISTER.changeResetValue(configuration.stackPointerAddress);
        RegisterFile.PC_REGISTER.changeResetValue(configuration.textBaseAddress);
        RegisterFile.PC_REGISTER.setValue(configuration.textBaseAddress);
        RegisterFile.resetRegisters();
    }
}
