package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.RegisterAccessNotice;
import rars.riscv.hardware.registers.Register;
import rars.settings.OtherSettings;

import java.util.function.Consumer;

/*
Copyright (c) 2003-2009,  Pete Sanderson and Kenneth Vollmar

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

// Adapted from RegisterFile class developed by Bumgarner et al in 2003.
// The FPU registers will be implemented by Register objects. Such objects
// can only hold int values, but we can use Float.floatToIntBits() to translate
// a 32 bit float value into its equivalent 32-bit int representation, and
// Float.intBitsToFloat() to bring it back.

/**
 * Represents the Floating Point Unit (FPU)
 *
 * @author Pete Sanderson
 * @version July 2005
 */
public final class FloatingPointRegisterFile {
    private static final RegisterBlock instance = new RegisterBlock(
        'f', new Register[]{
        new Register("ft0", 0, 0), new Register("ft1", 1, 0),
        new Register("ft2", 2, 0), new Register("ft3", 3, 0),
        new Register("ft4", 4, 0), new Register("ft5", 5, 0),
        new Register("ft6", 6, 0), new Register("ft7", 7, 0),
        new Register("fs0", 8, 0), new Register("fs1", 9, 0),
        new Register("fa0", 10, 0), new Register("fa1", 11, 0),
        new Register("fa2", 12, 0), new Register("fa3", 13, 0),
        new Register("fa4", 14, 0), new Register("fa5", 15, 0),
        new Register("fa6", 16, 0), new Register("fa7", 17, 0),
        new Register("fs2", 18, 0), new Register("fs3", 19, 0),
        new Register("fs4", 20, 0), new Register("fs5", 21, 0),
        new Register("fs6", 22, 0), new Register("fs7", 23, 0),
        new Register("fs8", 24, 0), new Register("fs9", 25, 0),
        new Register("fs10", 26, 0), new Register("fs11", 27, 0),
        new Register("ft8", 28, 0), new Register("ft9", 29, 0),
        new Register("ft10", 30, 0), new Register("ft11", 31, 0)
    }
    );

    private FloatingPointRegisterFile() {
    }

    /**
     * Sets the value of the FPU register given to the value given.
     *
     * @param reg
     *     Register to set the value of.
     * @param val
     *     The desired float value for the register.
     */
    public static void setRegisterToFloat(final int reg, final float val) {
        FloatingPointRegisterFile.updateRegisterInt(reg, Float.floatToRawIntBits(val));
    }

    /**
     * Gets the float value stored in the given FPU register.
     *
     * @param name
     *     Register to get the value of.
     * @return The float value stored by that register.
     */
    public static float getFloatFromRegister(final String name) {
        return Float.intBitsToFloat(FloatingPointRegisterFile.getValue(name));
    }

    /**
     * This method updates the FPU register value who's number is num. Note the
     * registers themselves hold an int value. There are helper methods available
     * to which you can give a float or double to store.
     *
     * @param num
     *     FPU register to set the value of.
     * @param val
     *     The desired int value for the register.
     */
    public static void updateRegisterInt(final int num, final int val) {
        final long lval = val | 0xFFFFFFFF_00000000L; // NAN box if used as float
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addFloatingPointRestore(
                num,
                FloatingPointRegisterFile.instance.updateRegister(num, lval)
            );
        } else {
            FloatingPointRegisterFile.instance.updateRegister(num, lval);
        }
    }

    public static void updateRegister(final int num, final long val) {
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addFloatingPointRestore(
                num,
                FloatingPointRegisterFile.instance.updateRegister(num, val)
            );
        } else {
            FloatingPointRegisterFile.instance.updateRegister(num, val);
        }
    }

    /**
     * Gets the raw int value actually stored in a Register. If you need a
     * float, use Float.intBitsToFloat() to get the equivent float.
     *
     * @param num
     *     The FPU register number.
     * @return The int value of the given register.
     */
    public static int getValue(final int num) {
        final long lval = FloatingPointRegisterFile.instance.getValue(num);
        if ((lval & 0xFFFFFFFF_00000000L) == 0xFFFFFFFF_00000000L) {
            return (int) lval; // If NaN-Boxed return value
        } else {
            return 0x7FC00000; // Otherwise NaN
        }
    }

    public static long getValueLong(final int num) {
        return FloatingPointRegisterFile.instance.getValue(num);
    }

    /**
     * Gets the raw int value actually stored in a Register. If you need a
     * float, use Float.intBitsToFloat() to get the equivalent float.
     *
     * @param name
     *     The FPU register name.
     * @return The int value of the given register.
     */
    public static int getValue(final String name) {
        final long lval = FloatingPointRegisterFile.instance.getValue(name);
        if ((lval & 0xFFFFFFFF_00000000L) == 0xFFFFFFFF_00000000L) {
            return (int) lval;
        } else {
            return 0x7FC00000;
        }
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     */
    public static Register[] getRegisters() {
        return FloatingPointRegisterFile.instance.getRegisters();
    }

    /**
     * Get register object corresponding to given name. If no match, return null.
     *
     * @param name
     *     The FPU register name, must be "f0" through "f31".
     * @return The register object,or null if not found.
     */
    public static Register getRegister(final String name) {
        return FloatingPointRegisterFile.instance.getRegister(name);
    }

    /**
     * Method to reinitialize the values of the registers.
     */
    public static void resetRegisters() {
        FloatingPointRegisterFile.instance.resetRegisters();
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will add the given Observer to each one.
     *
     * @param listener
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void addRegistersSubscriber(final @NotNull Consumer<? super RegisterAccessNotice> listener) {
        FloatingPointRegisterFile.instance.addRegistersObserver(listener);
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will delete the given Observer from each one.
     *
     * @param listener
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void deleteRegistersObserver(final @NotNull Consumer<? super RegisterAccessNotice> listener) {
        FloatingPointRegisterFile.instance.deleteRegistersSubscriber(listener);
    }
}
