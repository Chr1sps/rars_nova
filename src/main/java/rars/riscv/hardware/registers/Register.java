package rars.riscv.hardware.registers;

import org.jetbrains.annotations.NotNull;
import rars.notices.AccessNotice;
import rars.notices.RegisterAccessNotice;
import rars.util.ListenerDispatcher;

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
 * Abstraction to represent a register of a MIPS Assembler.
 *
 * @author Jason Bumgarner, Jason Shrewsbury, Ben Sherman
 * @version June 2003
 */
public class Register {
    public final @NotNull String name;
    public final int number;
    public final @NotNull ListenerDispatcher<@NotNull RegisterAccessNotice>.Hook registerChangeHook;
    private final @NotNull ListenerDispatcher<@NotNull RegisterAccessNotice> registerChangeDispatcher;
    private long resetValue;
    // volatile should be enough to allow safe multi-threaded access
    // w/o the use of synchronized methods. getValue and setValue
    // are the only methods here used by the register collection
    // (RegisterFile, ControlAndStatusRegisterFile, FloatingPointRegisterFile)
    // methods.
    private volatile long value;

    /**
     * Creates a new register with specified name, number, and value.
     *
     * @param name
     *     The name of the register.
     * @param number
     *     The number of the register.
     * @param initialValue
     *     The inital (and reset) value of the register.
     */
    public Register(final @NotNull String name, final int number, final long initialValue) {
        this.name = name;
        this.number = number;
        this.value = initialValue;
        this.resetValue = initialValue;
        this.registerChangeDispatcher = new ListenerDispatcher<>();
        this.registerChangeHook = this.registerChangeDispatcher.getHook();
    }

    /**
     * Returns the value of the Register. Observers are notified
     * of the READ operation.
     *
     * @return value The value of the Register.
     */
    public final synchronized long getValue() {
        this.registerChangeDispatcher.dispatch(new RegisterAccessNotice(AccessNotice.AccessType.READ, this));
        return this.getValueNoNotify();
    }

    /**
     * Returns the value of the Register. Observers are not notified.
     * Added for release 3.8.
     *
     * @return value The value of the Register.
     */
    public synchronized long getValueNoNotify() {
        return this.value;
    }

    /**
     * Returns the reset value of the Register.
     *
     * @return The reset (initial) value of the Register.
     */
    public long getResetValue() {
        return this.resetValue;
    }

    /**
     * Sets the value of the register to the val passed to it.
     * Observers are notified of the WRITE operation.
     *
     * @param val
     *     Value to set the Register to.
     * @return previous value of register
     */
    public synchronized long setValue(final long val) {
        final long old = this.value;
        this.value = val;
        this.registerChangeDispatcher.dispatch(new RegisterAccessNotice(AccessNotice.AccessType.WRITE, this));
        return old;
    }

    /**
     * Sets the value of the register to the val passed to it. This should only
     * be used to update registers not related to the current instruction.
     *
     * @param val
     *     Value to set the Register to.
     * @return previous value of register
     */
    public synchronized long setValueNoNotify(final long val) {
        final long old = this.value;
        this.value = val;
        return old;
    }

    /**
     * Resets the value of the register to the value it was constructed with.
     * Observers are not notified.
     */
    public synchronized void resetValue() {
        this.value = this.resetValue;
    }

    /**
     * Change the register's reset value; the value to which it will be
     * set when {@code resetValue()} is called.
     *
     * @param reset
     *     a long
     */
    public synchronized void changeResetValue(final long reset) {
        this.resetValue = reset;
    }
}
