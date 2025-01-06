package rars.riscv.hardware;

import org.jetbrains.annotations.NotNull;
import rars.Globals;
import rars.notices.RegisterAccessNotice;
import rars.riscv.hardware.registers.LinkedRegister;
import rars.riscv.hardware.registers.MaskedRegister;
import rars.riscv.hardware.registers.ReadOnlyRegister;
import rars.riscv.hardware.registers.Register;
import rars.settings.OtherSettings;
import rars.util.SimpleSubscriber;

import java.util.concurrent.Flow;

/*
Copyright (c) 2017-2019,  Benjamin Landers

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

/**
 * Represents the implemented control and status registers. The main classes are
 * fcsr (for floating point errors),
 * timers, and interrupt handling.
 *
 * @author Benjamin Landers
 * @version August 2017
 */
public final class ControlAndStatusRegisterFile {
    public static final int EXTERNAL_INTERRUPT = 0x100;
    public static final int TIMER_INTERRUPT = 0x10;
    public static final int SOFTWARE_INTERRUPT = 0x1;
    public static final int INTERRUPT_ENABLE = 0x1;

    public static final @NotNull MaskedRegister USTATUS;
    public static final @NotNull LinkedRegister FFLAGS;
    public static final @NotNull LinkedRegister FRM;
    public static final @NotNull MaskedRegister FCSR;

    public static final @NotNull Register UIE, UTVEC, USCRATCH, UEPC, UCAUSE, UTVAL, UIP;

    public static final @NotNull ReadOnlyRegister CYCLE, TIME, INSTRET;
    public static final @NotNull LinkedRegister CYCLEH, TIMEH, INSTRETH;

    private static final @NotNull RegisterBlock instance;

    static {
        // Remember to update the window tooltips when adding a CSR

        USTATUS = new MaskedRegister("ustatus", 0x000, 0, ~0x11);

        UIE = new Register("uie", 0x004, 0);
        UTVEC = new Register("utvec", 0x005, 0);
        USCRATCH = new Register("uscratch", 0x040, 0);
        UEPC = new Register("uepc", 0x041, 0);
        UCAUSE = new Register("ucause", 0x042, 0);
        UTVAL = new Register("utval", 0x043, 0);
        UIP = new Register("uip", 0x044, 0);

        FCSR = new MaskedRegister("fcsr", 0x003, 0, ~0xFF);

        FFLAGS = new LinkedRegister("fflags", 0x001, FCSR, 0x1F);
        FRM = new LinkedRegister("frm", 0x002, FCSR, 0xE0);

        CYCLE = new ReadOnlyRegister("cycle", 0xC00, 0);
        TIME = new ReadOnlyRegister("time", 0xC01, 0);
        INSTRET = new ReadOnlyRegister("instret", 0xC02, 0);

        CYCLEH = new LinkedRegister("cycleh", 0xC80, CYCLE, 0xFFFFFFFF_00000000L);
        TIMEH = new LinkedRegister("timeh", 0xC81, TIME, 0xFFFFFFFF_00000000L);
        INSTRETH = new LinkedRegister("instreth", 0xC82, INSTRET, 0xFFFFFFFF_00000000L);

        final Register[] tmp = {
            USTATUS,
            FFLAGS,
            FRM,
            FCSR,
            UIE,
            UTVEC,
            USCRATCH,
            UEPC,
            UCAUSE,
            UTVAL,
            UIP,
            CYCLE,
            TIME,
            INSTRET,
            CYCLEH,
            TIMEH,
            INSTRETH,
        };

        instance = new RegisterBlock('_', tmp); // prefix not used
    }

    private ControlAndStatusRegisterFile() {
    }

    /**
     * This method updates the register value
     *
     * @param num
     *     Number of register to set the value of.
     * @param val
     *     The desired value for the register.
     * @return old value in register prior to update
     */
    public static boolean updateRegister(final int num, final long val) {
        final var register = ControlAndStatusRegisterFile.instance.getRegister(num);
        return ControlAndStatusRegisterFile.updateRegister(register, val);
    }

    public static boolean updateRegister(final @NotNull Register register, final long value) {
        if (register instanceof ReadOnlyRegister) {
            return true;
        }
        if (register == CYCLEH || register == TIMEH || register == INSTRETH) {
            return true;
        }
        final var previousValue = register.setValue(value);
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addControlAndStatusRestore(register.number, previousValue);
        }
        return false;
    }

    /**
     * This method updates the register value
     *
     * @param name
     *     Name of register to set the value of.
     * @param val
     *     The desired value for the register.
     */
    public static boolean updateRegister(final @NotNull String name, final long val) {
        final var register = ControlAndStatusRegisterFile.instance.getRegister(name);
        return ControlAndStatusRegisterFile.updateRegister(register, val);
    }

    /**
     * This method updates the register value silently and bypasses read only
     *
     * @param num
     *     Number of register to set the value of.
     * @param val
     *     The desired value for the register.
     */
    public static void updateRegisterBackdoor(final int num, final long val) {
        if ((OtherSettings.getBackSteppingEnabled())) {
            Globals.program.getBackStepper().addControlAndStatusBackdoor(
                num,
                ControlAndStatusRegisterFile.instance.getRegister(num).setValueBackdoor(val)
            );
        } else {
            ControlAndStatusRegisterFile.instance.getRegister(num).setValueBackdoor(val);
        }
    }

    /**
     * This method updates the register value silently and bypasses read only
     *
     * @param name
     *     Name of register to set the value of.
     * @param val
     *     The desired value for the register.
     */
    public static void updateRegisterBackdoor(final String name, final long val) {
        ControlAndStatusRegisterFile.updateRegisterBackdoor(
            ControlAndStatusRegisterFile.instance.getRegister(name).number, val
        );
    }

    /**
     * ORs a register with a value
     *
     * @param num
     *     Number of register to change
     * @param val
     *     The value to OR with
     * @return a boolean
     */
    public static boolean orRegister(final int num, final long val) {
        return ControlAndStatusRegisterFile.updateRegister(
            num,
            ControlAndStatusRegisterFile.instance.getValue(num) | val
        );
    }

    /**
     * ORs a register with a value
     *
     * @param name
     *     Name of register to change
     * @param val
     *     The value to OR with
     */
    public static void orRegister(final String name, final long val) {
        ControlAndStatusRegisterFile.updateRegister(name, ControlAndStatusRegisterFile.instance.getValue(name) | val);
    }

    /**
     * Clears bits from a register according to a value
     *
     * @param num
     *     Number of register to change
     * @param val
     *     The value to clear by
     * @return a boolean
     */
    public static boolean clearRegister(final int num, final long val) {
        return ControlAndStatusRegisterFile.updateRegister(
            num,
            ControlAndStatusRegisterFile.instance.getValue(num) & ~val
        );
    }

    /**
     * Clears bits from a register according to a value
     *
     * @param name
     *     Name of register to change
     * @param val
     *     The value to clear by
     */
    public static void clearRegister(final String name, final long val) {
        ControlAndStatusRegisterFile.updateRegister(name, ControlAndStatusRegisterFile.instance.getValue(name) & ~val);
    }

    /**
     * Returns the value of the register
     *
     * @param num
     *     The register number.
     * @return The value of the given register. 0 for non-implemented registers
     */
    public static int getValue(final int num) {
        return (int) ControlAndStatusRegisterFile.instance.getValue(num);
    }

    /**
     * Returns the full value of the register
     *
     * @param num
     *     The register number.
     * @return The value of the given register. 0 for non-implemented registers
     */
    public static long getValueLong(final int num) {
        return ControlAndStatusRegisterFile.instance.getValue(num);
    }

    /**
     * Returns the value of the register
     *
     * @param name
     *     The register's name
     * @return The value of the given register. 0 for non-implemented registers
     */
    public static int getValue(final String name) {
        return (int) ControlAndStatusRegisterFile.instance.getValue(name);
    }

    /**
     * Returns the value of the register without notifying observers
     *
     * @param name
     *     The register's name
     * @return The value of the given register. 0 for non-implemented registers
     */
    public static long getValueNoNotify(final String name) {
        return ControlAndStatusRegisterFile.instance.getRegister(name).getValueNoNotify();
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     */
    public static Register[] getRegisters() {
        return ControlAndStatusRegisterFile.instance.getRegisters();
    }

    public static Register getRegister(final String name) {
        return ControlAndStatusRegisterFile.instance.getRegister(name);
    }

    /** Method to reinitialize the values of the registers. */
    public static void resetRegisters() {
        ControlAndStatusRegisterFile.instance.resetRegisters();
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will add the given Observer to each one.
     *
     * @param observer
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void addRegistersObserver(final SimpleSubscriber<? super RegisterAccessNotice> observer) {
        ControlAndStatusRegisterFile.instance.addRegistersObserver(observer);
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will delete the given Observer from each one.
     *
     * @param observer
     *     a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public static void deleteRegistersObserver(final Flow.Subscriber<? super RegisterAccessNotice> observer) {
        ControlAndStatusRegisterFile.instance.deleteRegistersSubscriber(observer);
    }

}
