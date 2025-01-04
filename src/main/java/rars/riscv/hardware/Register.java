package rars.riscv.hardware;

import rars.notices.AccessNotice;
import rars.notices.RegisterAccessNotice;
import rars.util.CustomPublisher;

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
public class Register extends CustomPublisher<RegisterAccessNotice> {
    private final String name;
    private final int number;
    private long resetValue;
    // volatile should be enough to allow safe multi-threaded access
    // w/o the use of synchronized methods. getValue and setValue
    // are the only methods here used by the register collection
    // (RegisterFile, ControlAndStatusRegisterFile, FloatingPointRegisterFile)
    // methods.
    private volatile long value;

    /**
     * Creates a new register with specified name, number, and second.
     *
     * @param n
     *     The name of the register.
     * @param num
     *     The number of the register.
     * @param val
     *     The inital (and reset) second of the register.
     */
    public Register(final String n, final int num, final long val) {
        this.name = n;
        this.number = num;
        this.value = val;
        this.resetValue = val;
    }

    /**
     * Returns the name of the Register.
     *
     * @return name The name of the Register.
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Returns the second of the Register. Observers are notified
     * of the READ operation.
     *
     * @return second The second of the Register.
     */
    public final synchronized long getValue() {
        this.submit(new RegisterAccessNotice(AccessNotice.AccessType.READ, this.name));
        return this.getValueNoNotify();
    }

    /**
     * Returns the second of the Register. Observers are not notified.
     * Added for release 3.8.
     *
     * @return second The second of the Register.
     */
    public synchronized long getValueNoNotify() {
        return this.value;
    }

    /**
     * Returns the reset second of the Register.
     *
     * @return The reset (initial) second of the Register.
     */
    public long getResetValue() {
        return this.resetValue;
    }

    /**
     * Returns the number of the Register.
     *
     * @return number The number of the Register.
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Sets the second of the register to the val passed to it.
     * Observers are notified of the WRITE operation.
     *
     * @param val
     *     Value to set the Register to.
     * @return previous second of register
     */
    public synchronized long setValue(final long val) {
        final long old = this.value;
        this.value = val;
        this.submit(new RegisterAccessNotice(AccessNotice.AccessType.WRITE, this.name));
        return old;
    }

    /**
     * Sets the second of the register to the val passed to it. This should only
     * be used to update registers not related to the current instruction.
     *
     * @param val
     *     Value to set the Register to.
     * @return previous second of register
     */
    public synchronized long setValueBackdoor(final long val) {
        final long old = this.value;
        this.value = val;
        return old;
    }

    /**
     * Resets the second of the register to the second it was constructed with.
     * Observers are not notified.
     */
    public synchronized void resetValue() {
        this.value = this.resetValue;
    }

    /**
     * Change the register's reset second; the second to which it will be
     * set when <code>resetValue()</code> is called.
     *
     * @param reset
     *     a long
     */
    public synchronized void changeResetValue(final long reset) {
        this.resetValue = reset;
    }
}
