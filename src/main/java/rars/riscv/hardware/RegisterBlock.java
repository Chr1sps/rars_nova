package rars.riscv.hardware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rars.notices.RegisterAccessNotice;
import rars.util.Binary;

import java.util.concurrent.Flow;

/*
Copyright (c) 2003-2017,  Pete Sanderson, Benjamin Landers and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu),
Benjamin Landers (benjaminrlanders@gmail.com)
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
 * Helper class for RegisterFile, FPRegisterFile, and CSRFile
 * <p>
 * Much of the implementation was ripped directly from RegisterFile
 *
 * @author Benjamin Landers
 * @version June 2017
 */
public class RegisterBlock {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Register[] regFile;
    private final char prefix;

    /**
     * <p>Constructor for RegisterBlock.</p>
     *
     * @param prefix    a char
     * @param registers an array of {@link Register} objects
     */
    protected RegisterBlock(final char prefix, final Register[] registers) {
        this.prefix = prefix;
        this.regFile = registers;
    }

    /**
     * This method updates the register value to val
     *
     * @param r   Register to set the value of.
     * @param val The desired value for the register.
     * @return a long
     */
    public static long updateRegister(final Register r, final long val) {
        if (r == null)
            return 0;
        return r.setValue(val);
    }

    /**
     * Method for displaying the register values for debugging.
     */
    public void showRegisters() {
        for (final Register r : this.regFile) {
            RegisterBlock.LOGGER.debug("Name: {}", r.getName());
            RegisterBlock.LOGGER.debug("Number: {}", r.getNumber());
            RegisterBlock.LOGGER.debug("Value: {}", r.getValue());
            RegisterBlock.LOGGER.debug('\n');
        }
    }

    /**
     * <p>updateRegister.</p>
     *
     * @param num a int
     * @param val a long
     * @return a long
     */
    public long updateRegister(final int num, final long val) {
        return RegisterBlock.updateRegister(this.getRegister(num), val);
    }

    /**
     * <p>updateRegister.</p>
     *
     * @param name a {@link java.lang.String} object
     * @param val  a long
     * @return a long
     */
    public long updateRegister(final String name, final long val) {
        return RegisterBlock.updateRegister(this.getRegister(name), val);
    }

    /**
     * Returns the value of the register.
     *
     * @param num The register's number.
     * @return The value of the given register.
     */
    public long getValue(final int num) {
        return this.getRegister(num).getValue();
    }

    /**
     * Returns the value of the register.
     *
     * @param name The register's name.
     * @return The value of the given register.
     */
    public long getValue(final String name) {
        return this.getRegister(name).getValue();
    }

    /**
     * Get a register from a number
     *
     * @param num the number to search for
     * @return the register for num or null if none exists
     */
    public Register getRegister(final int num) {
        for (final Register r : this.regFile) {
            if (r.getNumber() == num) {
                return r;
            }
        }
        return null;
    }

    /**
     * Get register object corresponding to given name. If no match, return null.
     *
     * @param name The register name, either in (prefix)(number) format or a direct
     *             name.
     * @return The register object,or null if not found.
     */
    public Register getRegister(final String name) {
        if (name.length() < 2)
            return null;

        // Handle a direct name
        for (final Register r : this.regFile) {
            if (r.getName().equals(name)) {
                return r;
            }
        }
        // Handle prefix case
        if (name.charAt(0) == this.prefix) {
            if (name.charAt(1) == 0) { // Ensure that it is a normal decimal number
                if (name.length() > 2)
                    return null;
                return this.getRegister(0);
            }

            final Integer num = Binary.stringToIntFast(name.substring(1));
            if (num == null)
                return null;
            return this.getRegister(num);
        }
        return null;
    }

    /**
     * For returning the set of registers.
     *
     * @return The set of registers.
     */
    public Register[] getRegisters() {
        return this.regFile;
    }

    /**
     * Method to reinitialize the values of the registers.
     */
    public void resetRegisters() {
        for (final Register r : this.regFile) {
            r.resetValue();
        }
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will add the given Observer to each one. Currently does not apply to Program
     * Counter.
     *
     * @param observer a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public void addRegistersObserver(final Flow.Subscriber<? super RegisterAccessNotice> observer) {
        for (final Register r : this.regFile) {
            r.subscribe(observer);
        }
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method
     * will delete the given Observer from each one. Currently does not apply to
     * Program
     * Counter.
     *
     * @param subscriber a {@link java.util.concurrent.Flow.Subscriber} object
     */
    public void deleteRegistersSubscriber(final Flow.Subscriber<? super RegisterAccessNotice> subscriber) {
        for (final Register r : this.regFile) {
            r.deleteSubscriber(subscriber);
        }
    }

}